package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.analysis.AddressWindow
import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.ConfidenceScoring
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.Premise
import squad.nononotaa.backend.analysis.PremiseFacts
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.ScoringInput
import squad.nononotaa.backend.analysis.ScoringResult
import squad.nononotaa.backend.analysis.bareUrl
import squad.nononotaa.backend.analysis.fold
import squad.nononotaa.backend.analysis.premiseFacts
import squad.nononotaa.backend.model.AssessedBy
import squad.nononotaa.backend.model.AuthorMatch
import squad.nononotaa.backend.model.DismissalReason
import squad.nononotaa.backend.model.PremiseKind
import squad.nononotaa.backend.model.PremiseStatus
import squad.nononotaa.backend.util.logger

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
        "Wzmianki, w których adres występuje w ostrzeżeniu lub z prośbą, by go nie używać, wymagają weryfikacji, " +
            "więc nie zostały uznane za potwierdzenie.",
    ),
    CONFLICTING_EMAIL(0, "Wzmianki zawierające adres e-mail innej osoby nie zostały uznane za potwierdzenie."),
    CLASSIFICATION_UNAVAILABLE(0, "Nie udało się sprawdzić, czy adres należy do giełdy, kontraktu lub innej usługi."),

    // its contribution is negative and computed per Attribution: the amount cut to reach the cap (P9)
    CLASSIFICATION_CAP(0, "Z tego powodu stopień pewności ograniczono, tak aby nie wskazywał wysokiego prawdopodobieństwa."),
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
                    premises = premises(candidate, mentions, input.identifiers),
                    entered = candidate.address in wallets,
                    unclassified = candidate in input.unclassified,
                )
            }
        logger.info(
            "Scored {} candidates from {} Premises with algorithm {}",
            attributions.size,
            attributions.sumOf { it.premises.size },
            ALGORITHM_VERSION,
        )
        return ScoringResult(attributions, ALGORITHM_VERSION)
    }

    /** The `confidence-2` assessment of one Premise by rule; task 02 calls it per Premise as the fallback. */
    fun assessPremise(
        mention: FoundMention,
        window: AddressWindow,
        identifiers: List<CaseIdentifier>,
    ): Premise {
        val facts = premiseFacts(mention, window, identifiers)
        // the rule order of P13: link, e-mail of another person, warning, kind
        val reason =
            when {
                !linked(facts) -> DismissalReason.NO_PERSON_LINK
                facts.conflictingEmail -> DismissalReason.CONFLICTING_EMAIL
                WARNING_WORDS.foundIn(fold(sentenceOf(window))) -> DismissalReason.WARNING
                else -> null
            }
        // a warning on a linked Premise is for the Analyst to review, not a dismissal (L2)
        val status =
            when (reason) {
                null -> PremiseStatus.COUNTED
                DismissalReason.WARNING -> PremiseStatus.FOR_REVIEW
                else -> PremiseStatus.DISMISSED
            }
        val kind = if (reason == null) kindOf(mention, window, identifiers, facts) else null
        val address = window.address ?: window.rawAddress
        return Premise(
            id = Premise.idOf(mention.id, address),
            mentionId = mention.id,
            address = address,
            window = window,
            facts = facts,
            status = status,
            kind = kind,
            points = kind?.let { KIND_RULES.getValue(it).contribution } ?: 0,
            dismissalReason = reason,
            assessedBy = AssessedBy.RULE,
        )
    }

    // a Mention without a window for the address (stored before confidence-2) gives no Premise
    private fun premises(
        candidate: Candidate,
        mentions: Map<String, FoundMention>,
        identifiers: List<CaseIdentifier>,
    ): List<Premise> = candidate.mentionIds
        .distinct()
        .mapNotNull(mentions::get)
        .mapNotNull { mention ->
            mention.addressWindows.firstOrNull { it.address == candidate.address }?.let { assessPremise(mention, it, identifiers) }
        }.sortedBy { it.id }

    private fun score(
        candidate: Candidate,
        premises: List<Premise>,
        entered: Boolean,
        unclassified: Boolean,
    ): ScoredAttribution {
        val applied = sortedMapOf<ScoringRule, List<Premise>>()
        if (entered) applied[ScoringRule.ENTERED_BY_ANALYST] = emptyList()
        val counted = premises.filter { it.status == PremiseStatus.COUNTED }
        if (counted.isEmpty()) {
            // Premises put aside for a warning or another e-mail have their own notes, so they are not "no link" (L2)
            val unlinked = premises.filter { it.dismissalReason == DismissalReason.NO_PERSON_LINK }
            if (premises.isEmpty() || unlinked.isNotEmpty()) applied[ScoringRule.NO_PERSON_LINK] = unlinked
        } else {
            val strongest = KIND_RULES.getValue(counted.maxBy { it.points }.kind!!)
            applied[strongest] = counted.filter { KIND_RULES[it.kind] == strongest }
            // a mass comment is too weak to confirm another source (D9-002)
            val confirming = counted.filter { it.points >= ScoringRule.DISCUSSION.contribution }
            val crossService = pairs(confirming).filter { (a, b) -> origin(a) != origin(b) }
            // a copy of the same sentence on another service is not independent (E7.3)
            val independent = crossService.filter { (a, b) -> sentence(a) != sentence(b) }
            when {
                independent.isNotEmpty() -> applied[ScoringRule.INDEPENDENT_SOURCES] = independent.flatMap { it.toList() }
                crossService.isNotEmpty() -> applied[ScoringRule.COPIED_TEXT] = crossService.flatMap { it.toList() }
                confirming.size > 1 -> applied[ScoringRule.SAME_SERVICE] = confirming
            }
            if (counted.none(::strongLink)) applied[ScoringRule.NICKNAME_ONLY] = counted
        }
        premises.filter { it.dismissalReason == DismissalReason.WARNING }.ifNotEmpty { applied[ScoringRule.WARNING_CONTEXT] = it }
        premises.filter { it.dismissalReason == DismissalReason.CONFLICTING_EMAIL }.ifNotEmpty { applied[ScoringRule.CONFLICTING_EMAIL] = it }
        if (unclassified) applied[ScoringRule.CLASSIFICATION_UNAVAILABLE] = emptyList()

        val uncapped = applied.keys.sumOf { it.contribution }.coerceIn(0, MAX)
        // an address that may belong to an exchange or a contract never reads as a high probability (P9)
        val cut = if (unclassified && uncapped > UNCLASSIFIED_CAP) UNCLASSIFIED_CAP - uncapped else 0
        if (cut < 0) applied[ScoringRule.CLASSIFICATION_CAP] = emptyList()
        val level = uncapped + cut
        logger.debug(
            "Scored candidate network={} level={} rules={} premises={} counted={}",
            candidate.network,
            level,
            applied.keys,
            premises.size,
            counted.size,
        )
        val rules =
            applied.map { (rule, triggers) ->
                val contribution = if (rule == ScoringRule.CLASSIFICATION_CAP) cut else rule.contribution
                AppliedRule(rule.name, contribution, triggers.map { it.id }.distinct().sorted())
            }
        return ScoredAttribution(
            candidate = candidate,
            confidenceLevel = level,
            appliedRules = rules,
            justification = applied.entries.joinToString(" ") { (rule, triggers) -> rule.sentence(triggers.map(::origin).distinct().sorted()) },
            premises = premises,
        )
    }

    // the author, the profile (with an author who is not someone else) or an identifier near the address (P4, P5, P7)
    private fun linked(facts: PremiseFacts) = facts.authorIsPerson == AuthorMatch.YES ||
        (facts.underProfile && facts.authorIsPerson != AuthorMatch.NO) ||
        (facts.identifierDistance != null && facts.identifierDistance <= LINK_DISTANCE)

    // the author is compared by nickname or login, so it is as weak as they are; the full name stays a second identifier as in confidence-1
    private fun strongLink(premise: Premise) = (premise.facts.underProfile && premise.facts.authorIsPerson != AuthorMatch.NO) ||
        premise.facts.identifierTypes.any { it !in WEAK_LINKS }

    private fun kindOf(
        mention: FoundMention,
        window: AddressWindow,
        identifiers: List<CaseIdentifier>,
        facts: PremiseFacts,
    ): PremiseKind {
        val text = fold("${mention.url} ${window.text}")
        return when {
            ownProfile(mention.url, identifiers, facts) -> PremiseKind.OWN_PROFILE
            MASS_COMMENT_WORDS.foundIn(text) -> PremiseKind.MASS_COMMENT
            SALE_OR_SETTLEMENT_WORDS.foundIn(text) -> PremiseKind.SALE_OR_SETTLEMENT
            else -> PremiseKind.DISCUSSION
        }
    }

    // a page named after the nickname may belong to a namesake, so it needs a second identifier anywhere in the window (D9-003)
    private fun ownProfile(
        url: String,
        identifiers: List<CaseIdentifier>,
        facts: PremiseFacts,
    ): Boolean {
        if (facts.authorIsPerson == AuthorMatch.NO) return false
        if (facts.underProfile) return true
        val firstSegment =
            bareUrl(url)
                .substringAfter('/', "")
                .substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
                .removePrefix("@")
        val nicknamePage =
            firstSegment.isNotEmpty() &&
                identifiers.any { it.type in WEAK_LINKS && it.value.trim().removePrefix("@").equals(firstSegment, ignoreCase = true) }
        return nicknamePage && facts.identifierTypes.any { it in SECOND_IDENTIFIERS }
    }

    private fun sentenceOf(window: AddressWindow): String {
        val text = window.text
        val addressEnd = window.addressStart + window.addressLength
        var start = 0
        for (boundary in SENTENCE_BOUNDARY.findAll(text)) {
            if (boundary.range.first >= addressEnd) return text.substring(start, boundary.range.first)
            start = boundary.range.last + 1
        }
        return text.substring(start)
    }

    private fun sentence(premise: Premise) = fold(sentenceOf(premise.window)).replace(NON_WORD, " ").trim()

    private fun origin(premise: Premise) = premise.facts.host ?: premise.facts.sourceName

    private fun pairs(premises: List<Premise>) = premises.flatMapIndexed { index, a -> premises.drop(index + 1).map { b -> a to b } }

    private inline fun List<Premise>.ifNotEmpty(action: (List<Premise>) -> Unit) {
        if (isNotEmpty()) action(this)
    }

    private class Words(
        vararg words: String,
    ) {
        private val pattern = Regex(words.joinToString("|", "(?<![\\p{L}\\p{N}])(?:", ")") { Regex.escape(fold(it)) })

        fun foundIn(folded: String) = pattern.containsMatchIn(folded)
    }

    companion object {
        const val ALGORITHM_VERSION = "confidence-2"
        private const val MAX = 100
        private const val UNCLASSIFIED_CAP = 59
        private const val LINK_DISTANCE = 200
        private val KIND_RULES =
            mapOf(
                PremiseKind.OWN_PROFILE to ScoringRule.OWN_PROFILE,
                PremiseKind.SALE_OR_SETTLEMENT to ScoringRule.SALE_OR_SETTLEMENT,
                PremiseKind.DISCUSSION to ScoringRule.DISCUSSION,
                PremiseKind.MASS_COMMENT to ScoringRule.MASS_COMMENT,
            )
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
        private val SENTENCE_BOUNDARY = Regex("(?<=[.!?])\\s+|\\n+")
        private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
        private val logger = logger()
    }
}
