package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.ConfidenceScoring
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.ScoringInput
import squad.nononotaa.backend.analysis.ScoringResult
import squad.nononotaa.backend.util.logger
import java.net.URI
import java.text.Normalizer

// declaration order is the order of the Justification sentences (D9-006)
enum class ScoringRule(
    val contribution: Int,
    private val template: String,
) {
    ENTERED_BY_ANALYST(0, "Adres został wpisany przez analityka jako dane sprawy; samo wpisanie nie jest dowodem."),
    OWN_PROFILE(80, "Adres został opublikowany na profilu badanej osoby ({hosts})."),
    SALE_OR_SETTLEMENT(60, "Adres podano w ogłoszeniu sprzedaży lub w kontekście rozliczenia ({hosts})."),
    DISCUSSION(40, "Adres pojawił się w dyskusji ({hosts}); taki ślad wymaga potwierdzenia drugim źródłem."),
    MASS_COMMENT(10, "Adres pojawił się we wpisie konkursowym, promocyjnym lub typu giveaway ({hosts}), co jest słabą przesłanką."),
    NO_PERSON_LINK(0, "Żadna wzmianka nie łączy tego adresu z danymi badanej osoby."),
    INDEPENDENT_SOURCES(15, "Niezależne źródła różnego pochodzenia ({hosts}) wskazują na ten sam adres."),
    SAME_SERVICE(0, "Kolejne wzmianki z tego samego serwisu nie są dodatkowym potwierdzeniem."),
    COPIED_TEXT(0, "Wzmianka w innym serwisie powtarza tę samą treść, więc nie jest niezależnym potwierdzeniem."),
    NICKNAME_ONLY(0, "Adres łączy z badaną osobą tylko nick lub login, które mogą należeć do innej osoby."),
    WARNING_CONTEXT(
        0,
        "Wzmianki, w których adres występuje w ostrzeżeniu lub z prośbą, by go nie używać, nie zostały uznane za potwierdzenie.",
    ),
    CONFLICTING_EMAIL(0, "Wzmianki zawierające adres e-mail innej osoby nie zostały uznane za potwierdzenie."),
    CLASSIFICATION_UNAVAILABLE(0, "Nie udało się sprawdzić, czy adres należy do giełdy, kontraktu lub innej usługi."),
    ;

    fun sentence(hosts: List<String>) = template.replace("{hosts}", hosts.joinToString(", "))
}

@Service
class ConfidenceScoringService : ConfidenceScoring {
    override fun score(input: ScoringInput): ScoringResult {
        val mentions = input.mentions.associateBy { it.id }
        val wallets = input.identifiers.filter { it.type == IdentifierType.WALLET_ADDRESS }.map { it.value.trim() }.toSet()
        val attributions =
            (input.accepted + input.unclassified).map { candidate ->
                score(
                    candidate = candidate,
                    readings = candidate.mentionIds.mapNotNull(mentions::get).map { read(it, candidate.address, input.identifiers) },
                    entered = candidate.address in wallets,
                    unclassified = candidate in input.unclassified,
                )
            }
        logger.info("Scored {} candidates with algorithm {}", attributions.size, ALGORITHM_VERSION)
        return ScoringResult(attributions, ALGORITHM_VERSION)
    }

    private fun score(
        candidate: Candidate,
        readings: List<Reading>,
        entered: Boolean,
        unclassified: Boolean,
    ): ScoredAttribution {
        val applied = sortedMapOf<ScoringRule, List<String>>()
        if (entered) applied[ScoringRule.ENTERED_BY_ANALYST] = emptyList()
        val supporting = readings.filter { it.supports }
        if (supporting.isEmpty()) {
            applied[ScoringRule.NO_PERSON_LINK] = emptyList()
        } else {
            val strongest = supporting.maxBy { it.kind.contribution }.kind
            applied[strongest] = hosts(supporting.filter { it.kind == strongest })
            val crossService = supporting.flatMap { a -> supporting.map { b -> a to b } }.filter { (a, b) -> a.origin != b.origin }
            // a copy of the same sentence on another service is not independent (E7.3)
            val independent = crossService.filter { (a, b) -> a.sentence != b.sentence }
            when {
                independent.isNotEmpty() -> applied[ScoringRule.INDEPENDENT_SOURCES] = hosts(independent.map { it.first })
                crossService.isNotEmpty() -> applied[ScoringRule.COPIED_TEXT] = emptyList()
                supporting.size > 1 -> applied[ScoringRule.SAME_SERVICE] = emptyList()
            }
            if (supporting.all { WEAK_LINKS.containsAll(it.linked) }) applied[ScoringRule.NICKNAME_ONLY] = emptyList()
        }
        if (readings.any { it.warning }) applied[ScoringRule.WARNING_CONTEXT] = emptyList()
        if (readings.any { it.conflictingEmail }) applied[ScoringRule.CONFLICTING_EMAIL] = emptyList()
        if (unclassified) applied[ScoringRule.CLASSIFICATION_UNAVAILABLE] = emptyList()

        val level = applied.keys.sumOf { it.contribution }.coerceIn(0, MAX)
        logger.debug("Scored candidate network={} level={} rules={}", candidate.network, level, applied.keys)
        return ScoredAttribution(
            candidate = candidate,
            confidenceLevel = level,
            appliedRules = applied.keys.map { AppliedRule(it.name, it.contribution) },
            justification = applied.entries.joinToString(" ") { (rule, hosts) -> rule.sentence(hosts) },
        )
    }

    private fun read(
        mention: FoundMention,
        address: String,
        identifiers: List<CaseIdentifier>,
    ): Reading {
        val context = mention.context.orEmpty()
        val linked = identifiers.filter { it.type != IdentifierType.WALLET_ADDRESS && present(it, mention) }.map { it.type }.toSet()
        val sentence = fold(sentenceWith(context, address))
        return Reading(
            origin = origin(mention.url),
            kind = kind(mention, identifiers, linked),
            linked = linked,
            warning = WARNING_WORDS.foundIn(sentence),
            conflictingEmail = conflictingEmail(context, identifiers),
            sentence = sentence.replace(NON_WORD, " ").trim(),
        )
    }

    private fun present(
        identifier: CaseIdentifier,
        mention: FoundMention,
    ): Boolean {
        val value = identifier.value.trim()
        if (value.isEmpty()) return false
        if (identifier.type == IdentifierType.PROFILE_URL) {
            val profile = bareUrl(value)
            return under(bareUrl(mention.url), profile) || mention.context.orEmpty().contains(profile, ignoreCase = true)
        }
        return mention.url.contains(value, ignoreCase = true) || mention.context.orEmpty().contains(value, ignoreCase = true)
    }

    private fun kind(
        mention: FoundMention,
        identifiers: List<CaseIdentifier>,
        linked: Set<IdentifierType>,
    ): ScoringRule {
        val text = fold("${mention.url} ${mention.context.orEmpty()}")
        return when {
            ownProfile(mention.url, identifiers, linked) -> ScoringRule.OWN_PROFILE
            MASS_COMMENT_WORDS.foundIn(text) -> ScoringRule.MASS_COMMENT
            SALE_OR_SETTLEMENT_WORDS.foundIn(text) -> ScoringRule.SALE_OR_SETTLEMENT
            else -> ScoringRule.DISCUSSION
        }
    }

    // a page named after the nickname may belong to a namesake, so it needs a second identifier (D9-003)
    private fun ownProfile(
        url: String,
        identifiers: List<CaseIdentifier>,
        linked: Set<IdentifierType>,
    ): Boolean {
        val page = bareUrl(url)
        if (identifiers.any { it.type == IdentifierType.PROFILE_URL && it.value.isNotBlank() && under(page, bareUrl(it.value)) }) return true
        val firstSegment =
            page
                .substringAfter('/', "")
                .substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
                .removePrefix("@")
        val nicknamePage =
            firstSegment.isNotEmpty() &&
                identifiers.any { it.type in WEAK_LINKS && it.value.trim().removePrefix("@").equals(firstSegment, ignoreCase = true) }
        return nicknamePage && linked.any { it in SECOND_IDENTIFIERS }
    }

    private fun conflictingEmail(
        context: String,
        identifiers: List<CaseIdentifier>,
    ): Boolean {
        val own = identifiers.filter { it.type == IdentifierType.EMAIL }.map { it.value.trim().lowercase() }.filter { it.isNotEmpty() }
        if (own.isEmpty()) return false
        val found = EMAIL.findAll(context).map { it.value.lowercase() }.toList()
        return found.isNotEmpty() && found.none { it in own }
    }

    private fun sentenceWith(
        context: String,
        address: String,
    ): String {
        val at = context.indexOf(address, ignoreCase = true)
        if (at < 0) return context
        var start = 0
        for (boundary in SENTENCE_BOUNDARY.findAll(context)) {
            if (boundary.range.first >= at + address.length) return context.substring(start, boundary.range.first)
            start = boundary.range.last + 1
        }
        return context.substring(start)
    }

    private fun hosts(readings: List<Reading>) = readings.map { it.origin }.distinct().sorted()

    private fun origin(url: String) = runCatching { URI(url).host }
        .getOrNull()
        ?.lowercase()
        ?.removePrefix("www.")
        ?: url

    private fun bareUrl(url: String) = url
        .trim()
        .lowercase()
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')

    private fun under(
        url: String,
        prefix: String,
    ) = url == prefix || listOf('/', '?', '#').any { url.startsWith("$prefix$it") }

    private data class Reading(
        val origin: String,
        val kind: ScoringRule,
        val linked: Set<IdentifierType>,
        val warning: Boolean,
        val conflictingEmail: Boolean,
        val sentence: String,
    ) {
        // the full name never links an address to the person, and neither does the Analyst's wallet (D9-004)
        val supports = linked.any { it != IdentifierType.FULL_NAME } && !warning && !conflictingEmail
    }

    private class Words(
        vararg words: String,
    ) {
        private val pattern = Regex(words.joinToString("|", "(?<![\\p{L}\\p{N}])(?:", ")") { Regex.escape(fold(it)) })

        fun foundIn(folded: String) = pattern.containsMatchIn(folded)
    }

    companion object {
        const val ALGORITHM_VERSION = "confidence-1"
        private const val MAX = 100
        private val DIACRITICS = Regex("\\p{M}+")
        private val WHITESPACE = Regex("\\s+")
        private val WEAK_LINKS = setOf(IdentifierType.NICKNAME, IdentifierType.LOGIN)
        private val SECOND_IDENTIFIERS = setOf(IdentifierType.EMAIL, IdentifierType.MESSENGER_ID, IdentifierType.FULL_NAME)
        private val MASS_COMMENT_WORDS = Words("giveaway", "konkurs", "rozdani", "rozdajemy", "airdrop", "wygraj", "promo")
        private val SALE_OR_SETTLEMENT_WORDS =
            Words("ogłosz", "sprzeda", "kupi", "ofert", "p2p", "płatnoś", "zapłat", "wpłat", "przelew", "rozlicz", "faktur")
        private val WARNING_WORDS =
            Words(
                "oszust",
                "scam",
                "podszyw",
                "ostrzeg",
                "ostrzeż",
                "fałszyw",
                "fake",
                "phishing",
                "wyłudz",
                "nie wpłacaj",
                "nie wysyłaj",
                "nie używa",
                "nie używaj",
            )
        private val EMAIL = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        private val SENTENCE_BOUNDARY = Regex("(?<=[.!?])\\s+|\\n+")
        private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
        private val logger = logger()

        private fun fold(text: String) = Normalizer
            .normalize(text.lowercase().replace('ł', 'l'), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace(WHITESPACE, " ")
    }
}
