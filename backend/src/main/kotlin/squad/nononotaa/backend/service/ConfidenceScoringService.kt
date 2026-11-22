package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.analysis.AddressWindow
import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.ConfidenceScoring
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.KindProbabilities
import squad.nononotaa.backend.analysis.Premise
import squad.nononotaa.backend.analysis.PremiseAnswers
import squad.nononotaa.backend.analysis.PremiseFacts
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.ScoringInput
import squad.nononotaa.backend.analysis.ScoringResult
import squad.nononotaa.backend.analysis.YesNoProbabilities
import squad.nononotaa.backend.analysis.bareUrl
import squad.nononotaa.backend.analysis.fold
import squad.nononotaa.backend.analysis.premiseFacts
import squad.nononotaa.backend.analysis.premiseWindows
import squad.nononotaa.backend.model.AssessedBy
import squad.nononotaa.backend.model.AssessmentMode
import squad.nononotaa.backend.model.AuthorMatch
import squad.nononotaa.backend.model.DismissalReason
import squad.nononotaa.backend.model.ModelQuestion
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
    OWN_DECLARATION(80, "Autor wpisu przedstawił ten adres jako własny ({hosts})."),
    SALE_OR_SETTLEMENT(60, "Adres podano w ogłoszeniu sprzedaży lub w kontekście rozliczenia ({hosts})."),
    DISCUSSION(40, "Adres pojawił się w dyskusji ({hosts}); taki ślad wymaga potwierdzenia drugim źródłem."),
    MASS_COMMENT(10, "Adres pojawił się we wpisie konkursowym, promocyjnym lub typu giveaway ({hosts}), co jest słabą przesłanką."),
    NO_PERSON_LINK(0, "Żadna wzmianka nie łączy tego adresu z danymi badanej osoby."),
    DECLARED_BY_ANOTHER_PERSON(0, "Wzmianki, w których ten adres jako własny podaje inna osoba, nie zostały uznane za potwierdzenie."),
    INDEPENDENT_SOURCES(15, "Niezależne źródła różnego pochodzenia ({hosts}) wskazują na ten sam adres."),
    SAME_SERVICE(0, "Kolejne wzmianki z tego samego serwisu nie są dodatkowym potwierdzeniem."),
    COPIED_TEXT(0, "Wzmianka w innym serwisie powtarza tę samą treść, więc nie jest niezależnym potwierdzeniem."),
    NICKNAME_ONLY(0, "Adres łączy z badaną osobą tylko nick lub login, które mogą należeć do innej osoby."),
    WITHDRAWN_ADDRESS(0, "Autor wpisu podał, że ten adres był jego, ale już go nie używa."),
    FOR_REVIEW(
        0,
        "Części wzmianek nie dało się ocenić jednoznacznie, więc wymagają weryfikacji i liczą się co najwyżej jak dyskusja.",
    ),
    WARNING_CONTEXT(
        0,
        "Wzmianki, w których adres występuje w ostrzeżeniu lub z prośbą, by go nie używać, wymagają weryfikacji, " +
            "więc nie zostały uznane za potwierdzenie.",
    ),
    WARNING_DISMISSED(
        0,
        "Wzmianki, w których adres występuje w ostrzeżeniu lub z prośbą, by go nie używać, nie zostały uznane za potwierdzenie.",
    ),
    CONFLICTING_EMAIL(0, "Wzmianki zawierające adres e-mail innej osoby nie zostały uznane za potwierdzenie."),
    CLASSIFICATION_UNAVAILABLE(0, "Nie udało się sprawdzić, czy adres należy do giełdy, kontraktu lub innej usługi."),

    // its contribution is negative and computed per Attribution: the amount cut to reach the cap (P9)
    CLASSIFICATION_CAP(0, "Z tego powodu stopień pewności ograniczono, tak aby nie wskazywał wysokiego prawdopodobieństwa."),
    MODEL_ASSISTED(0, "Rodzaj wpisu i ostrzeżenia oceniono z pomocą modelu; punkty przyznały stałe reguły."),
    ;

    fun sentence(hosts: List<String>) = template.replace("{hosts}", hosts.joinToString(", "))
}

@Service
class ConfidenceScoringService : ConfidenceScoring {
    override fun score(input: ScoringInput): ScoringResult {
        val mentions = input.mentions.associateBy { it.id }
        val wallets = input.identifiers.filter { it.type == IdentifierType.WALLET_ADDRESS }.map { it.value.trim() }.toSet()
        // the model is on: confidence-3, also when it answered for no Premise (P22); with the model off answers are ignored
        val withModel = input.mode == AssessmentMode.WITH_MODEL
        val version = if (withModel) MODEL_ALGORITHM_VERSION else ALGORITHM_VERSION
        val answers = if (withModel) input.answers else emptyMap()
        val attributions =
            (input.accepted + input.unclassified).map { candidate ->
                score(
                    candidate = candidate,
                    premises = premises(candidate, mentions, input.identifiers, answers),
                    entered = candidate.address in wallets,
                    unclassified = candidate in input.unclassified,
                )
            }
        val premises = attributions.flatMap { it.premises }
        logger.info(
            "Scored {} candidates from {} Premises ({} assessed with the model) with algorithm {}",
            attributions.size,
            premises.size,
            premises.count { it.assessedBy == AssessedBy.MODEL },
            version,
        )
        return ScoringResult(attributions, version)
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

    /**
     * The `confidence-3` assessment of one Premise from the model's [answers] (D9, P13). Code keeps the link, the e-mail
     * and the points; the model only reads the kind, the declaration and the warning, and never makes a link (D6).
     */
    private fun assessPremise(
        mention: FoundMention,
        window: AddressWindow,
        identifiers: List<CaseIdentifier>,
        answers: PremiseAnswers,
    ): Premise {
        val facts = premiseFacts(mention, window, identifiers)
        val assessment = assessByModel(mention.url, identifiers, facts, answers)
        val address = window.address ?: window.rawAddress
        return Premise(
            id = Premise.idOf(mention.id, address),
            mentionId = mention.id,
            address = address,
            window = window,
            facts = facts,
            status = assessment.status,
            kind = assessment.kind,
            points = assessment.kind?.let { KIND_RULES.getValue(it).contribution } ?: 0,
            dismissalReason = assessment.reason,
            assessedBy = AssessedBy.MODEL,
            answers = answers,
            undetermined = assessment.undetermined,
            withdrawn = assessment.withdrawn,
        )
    }

    // the confidence-3 rule table, steps 1 to 7 in the order of P13
    private fun assessByModel(
        url: String,
        identifiers: List<CaseIdentifier>,
        facts: PremiseFacts,
        answers: PremiseAnswers,
    ): ModelAssessment {
        val declaration = yesNo(answers.declaration)
        val warning = yesNo(answers.warning)
        val author = facts.authorIsPerson
        // 1. the model never makes the link (D6); a profile page with a known other author is a link fact here (P7)
        if (!linked(facts)) {
            return when {
                facts.underProfile && author == AuthorMatch.NO && declaration == Answer.YES ->
                    ModelAssessment.dismissed(DismissalReason.DECLARED_BY_ANOTHER_PERSON)

                declaration == Answer.YES -> ModelAssessment(PremiseStatus.FOR_REVIEW, null, DismissalReason.NO_PERSON_LINK)

                else -> ModelAssessment.dismissed(DismissalReason.NO_PERSON_LINK)
            }
        }
        // 2.-5. the e-mail (code only), the dismissals on determined answers, the withdrawn own address (D10)
        when {
            facts.conflictingEmail -> return ModelAssessment.dismissed(DismissalReason.CONFLICTING_EMAIL)

            warning == Answer.YES && declaration != Answer.YES -> return ModelAssessment.dismissed(DismissalReason.WARNING)

            declaration == Answer.YES && author == AuthorMatch.NO ->
                return ModelAssessment.dismissed(DismissalReason.DECLARED_BY_ANOTHER_PERSON)

            declaration == Answer.YES && warning == Answer.YES && author == AuthorMatch.YES ->
                return ModelAssessment(PremiseStatus.COUNTED, PremiseKind.OWN_DECLARATION, null, withdrawn = true)
        }
        // 6. the own profile is code; otherwise the model's kind, which a declaration alone does not raise
        val ownProfile = ownProfile(url, identifiers, facts)
        val modelKind = kindOf(answers.kind)
        val kind =
            when {
                ownProfile -> PremiseKind.OWN_PROFILE
                modelKind != null -> modelKind
                topKind(answers.kind) == PremiseKind.MASS_COMMENT -> PremiseKind.MASS_COMMENT
                else -> PremiseKind.DISCUSSION
            }
        // 7. an undetermined answer that could still change the status (P13)
        val declarationMatters = author == AuthorMatch.NO || (author == AuthorMatch.YES && warning == Answer.UNDETERMINED)
        val undetermined =
            buildList {
                if (!ownProfile && modelKind == null) add(ModelQuestion.KIND)
                if (declaration == Answer.UNDETERMINED && declarationMatters) add(ModelQuestion.DECLARATION)
                if (warning == Answer.UNDETERMINED) add(ModelQuestion.WARNING)
            }
        if (undetermined.isEmpty()) return ModelAssessment(PremiseStatus.COUNTED, kind, null)
        // at most a discussion; a mass comment stays a mass comment
        val capped = if (KIND_RULES.getValue(kind).contribution > ScoringRule.DISCUSSION.contribution) PremiseKind.DISCUSSION else kind
        return ModelAssessment(PremiseStatus.FOR_REVIEW, capped, null, undetermined)
    }

    // a Premise without an answer is assessed by the confidence-2 rules (fallback, D13)
    private fun premises(
        candidate: Candidate,
        mentions: Map<String, FoundMention>,
        identifiers: List<CaseIdentifier>,
        answers: Map<String, PremiseAnswers>,
    ): List<Premise> = premiseWindows(candidate, mentions).map { (mention, window) ->
        answers[Premise.idOf(mention.id, candidate.address)]?.let { assessPremise(mention, window, identifiers, it) }
            ?: assessPremise(mention, window, identifiers)
    }

    private fun score(
        candidate: Candidate,
        premises: List<Premise>,
        entered: Boolean,
        unclassified: Boolean,
    ): ScoredAttribution {
        val applied = sortedMapOf<ScoringRule, List<Premise>>()
        if (entered) applied[ScoringRule.ENTERED_BY_ANALYST] = emptyList()
        // a Premise for review that still gives points counts at its capped kind (P8)
        val counted = premises.filter { it.status == PremiseStatus.COUNTED || (it.status == PremiseStatus.FOR_REVIEW && it.points > 0) }
        if (counted.isEmpty()) {
            // Premises put aside for a warning or another e-mail have their own notes, so they are not "no link" (L2)
            val unlinked = premises.filter { it.dismissalReason == DismissalReason.NO_PERSON_LINK }
            if (premises.isEmpty() || unlinked.isNotEmpty()) applied[ScoringRule.NO_PERSON_LINK] = unlinked
        } else {
            // of two kinds with the same points, the one declared first (own profile before own declaration)
            val strongest =
                counted
                    .map { KIND_RULES.getValue(it.kind!!) }
                    .maxWith(compareBy<ScoringRule> { it.contribution }.thenByDescending { it.ordinal })
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
        premises
            .filter { it.dismissalReason == DismissalReason.DECLARED_BY_ANOTHER_PERSON }
            .ifNotEmpty { applied[ScoringRule.DECLARED_BY_ANOTHER_PERSON] = it }
        premises.filter { it.withdrawn }.ifNotEmpty { applied[ScoringRule.WITHDRAWN_ADDRESS] = it }
        // a keyword warning on a linked Premise is for review with its own note (L2); the other Premises for review share one
        val (warned, forReview) = premises.filter { it.status == PremiseStatus.FOR_REVIEW }.partition { it.dismissalReason == DismissalReason.WARNING }
        forReview.ifNotEmpty { applied[ScoringRule.FOR_REVIEW] = it }
        warned.ifNotEmpty { applied[ScoringRule.WARNING_CONTEXT] = it }
        premises
            .filter { it.status == PremiseStatus.DISMISSED && it.dismissalReason == DismissalReason.WARNING }
            .ifNotEmpty { applied[ScoringRule.WARNING_DISMISSED] = it }
        premises.filter { it.dismissalReason == DismissalReason.CONFLICTING_EMAIL }.ifNotEmpty { applied[ScoringRule.CONFLICTING_EMAIL] = it }
        if (unclassified) applied[ScoringRule.CLASSIFICATION_UNAVAILABLE] = emptyList()

        val uncapped = applied.keys.sumOf { it.contribution }.coerceIn(0, MAX)
        // an address that may belong to an exchange or a contract never reads as a high probability (P9)
        val cut = if (unclassified && uncapped > UNCLASSIFIED_CAP) UNCLASSIFIED_CAP - uncapped else 0
        if (cut < 0) applied[ScoringRule.CLASSIFICATION_CAP] = emptyList()
        premises.filter { it.assessedBy == AssessedBy.MODEL }.ifNotEmpty { applied[ScoringRule.MODEL_ASSISTED] = it }
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

    // the thresholds of D9 and P12; the word follows the threshold, not a rounded number (P28)
    private fun yesNo(probabilities: YesNoProbabilities) = when {
        probabilities.yes >= YES_THRESHOLD -> Answer.YES
        probabilities.yes <= NO_THRESHOLD -> Answer.NO
        else -> Answer.UNDETERMINED
    }

    // on a tie the option asked first wins, so the result depends on the answers alone
    private fun topOption(probabilities: KindProbabilities) = listOf(
        PremiseKind.OWN_DECLARATION to probabilities.ownDeclaration,
        PremiseKind.SALE_OR_SETTLEMENT to probabilities.saleOrSettlement,
        PremiseKind.DISCUSSION to probabilities.discussion,
        PremiseKind.MASS_COMMENT to probabilities.massComment,
    ).maxBy { it.second }

    private fun topKind(probabilities: KindProbabilities) = topOption(probabilities).first

    private fun kindOf(probabilities: KindProbabilities) = topOption(probabilities).takeIf { it.second >= KIND_THRESHOLD }?.first

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

    private enum class Answer { YES, NO, UNDETERMINED }

    private data class ModelAssessment(
        val status: PremiseStatus,
        val kind: PremiseKind?,
        val reason: DismissalReason?,
        val undetermined: List<ModelQuestion> = emptyList(),
        val withdrawn: Boolean = false,
    ) {
        companion object {
            fun dismissed(reason: DismissalReason) = ModelAssessment(PremiseStatus.DISMISSED, null, reason)
        }
    }

    private class Words(
        vararg words: String,
    ) {
        private val pattern = Regex(words.joinToString("|", "(?<![\\p{L}\\p{N}])(?:", ")") { Regex.escape(fold(it)) })

        fun foundIn(folded: String) = pattern.containsMatchIn(folded)
    }

    companion object {
        const val ALGORITHM_VERSION = "confidence-2"
        const val MODEL_ALGORITHM_VERSION = "confidence-3"
        private const val YES_THRESHOLD = 0.7
        private const val NO_THRESHOLD = 0.3
        private const val KIND_THRESHOLD = 0.5
        private const val MAX = 100
        private const val UNCLASSIFIED_CAP = 59
        private const val LINK_DISTANCE = 200
        private val KIND_RULES =
            mapOf(
                PremiseKind.OWN_PROFILE to ScoringRule.OWN_PROFILE,
                PremiseKind.OWN_DECLARATION to ScoringRule.OWN_DECLARATION,
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
