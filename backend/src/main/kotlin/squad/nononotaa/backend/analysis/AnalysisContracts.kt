package squad.nononotaa.backend.analysis

import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.AssessedBy
import squad.nononotaa.backend.model.AssessmentMode
import squad.nononotaa.backend.model.AuthorMatch
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.DismissalReason
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.ModelQuestion
import squad.nononotaa.backend.model.PremiseKind
import squad.nononotaa.backend.model.PremiseStatus
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.SourceStatus
import java.time.Instant

enum class AnalysisStage { SEARCH, CANDIDATE_PREPARATION, EXCLUSION, SCORING }

enum class IdentifierType { NICKNAME, LOGIN, EMAIL, MESSENGER_ID, PROFILE_URL, WALLET_ADDRESS, FULL_NAME }

data class CaseIdentifier(
    val type: IdentifierType,
    val value: String,
)

data class AnalysisRequest(
    val caseId: String,
    val trigger: AnalysisTrigger,
    val identifiers: List<CaseIdentifier>,
)

/**
 * The text around one detected address. `text.substring(addressStart, addressStart + addressLength)` is the
 * address as written ([rawAddress]); indices are Kotlin `String` indices (UTF-16 code units).
 * [address] is the canonical form from `AddressValidator`, null when the address is invalid.
 */
data class AddressWindow(
    val rawAddress: String,
    val address: String?,
    val text: String,
    val addressStart: Int,
    val addressLength: Int,
)

data class FoundMention @JvmOverloads constructor(
    val id: String,
    val sourceName: String,
    val url: String,
    val collectedAt: Instant,
    val context: String?,
    val detectedAddresses: List<String>,
    val author: String? = null,
    // Mentions stored before algorithm confidence-2 have no windows
    val addressWindows: List<AddressWindow> = emptyList(),
)

data class SourceReport(
    val sourceName: String,
    val status: SourceStatus,
    val synthetic: Boolean,
    val checkedAt: Instant?,
)

data class SearchResult(
    val mentions: List<FoundMention>,
    val sources: List<SourceReport>,
)

data class Candidate(
    val network: String,
    val address: String,
    val mentionIds: List<String>,
)

data class RejectedCandidate(
    val mentionId: String,
    val value: String,
    val reason: String,
)

data class PreparedCandidates(
    val valid: List<Candidate>,
    val rejected: List<RejectedCandidate>,
)

data class ExcludedCandidate(
    val candidate: Candidate,
    val reason: ExclusionReason,
    val classificationSource: String,
)

data class ExclusionResult(
    val accepted: List<Candidate>,
    val excluded: List<ExcludedCandidate>,
    val unclassified: List<Candidate>,
)

data class AppliedRule @JvmOverloads constructor(
    val rule: String,
    val contribution: Int,
    // Analyses stored before algorithm confidence-2 have no Premises
    val premiseIds: List<String> = emptyList(),
)

/** Any change of the wording of a question is a new question-set version (P21). */
const val QUESTION_SET_VERSION = "premise-questions-1"

/** Raw probabilities of a yes/no question, as the model returned them. Thresholds are applied in scoring (D9). */
data class YesNoProbabilities(
    val yes: Double,
    val no: Double,
)

/** Raw probability of every option of `rodzaj_wpisu`, as the model returned them. */
data class KindProbabilities(
    val ownDeclaration: Double,
    val saleOrSettlement: Double,
    val discussion: Double,
    val massComment: Double,
)

/** The three answers of the decision model for one Premise. Numbers only; the model returns no text (D5). */
data class PremiseAnswers @JvmOverloads constructor(
    val kind: KindProbabilities,
    val declaration: YesNoProbabilities,
    val warning: YesNoProbabilities,
    val requestId: String? = null,
)

/**
 * What the assessment port gets about one Premise: the unmasked window [text], the assessed [address] and the code
 * facts. Never the URL, the Case id or the Analysis results; the adapter masks [text] before it leaves the process.
 */
data class PremiseToAssess(
    val premiseId: String,
    val address: String,
    val text: String,
    val facts: PremiseFacts,
)

/** [answers] by Premise id; a Premise without an entry got no answer and falls back to the rules (D13). */
data class PremiseAssessments @JvmOverloads constructor(
    val answers: Map<String, PremiseAnswers> = emptyMap(),
    val modelVersion: String? = null,
)

data class ScoringInput @JvmOverloads constructor(
    val accepted: List<Candidate>,
    val unclassified: List<Candidate>,
    val mentions: List<FoundMention>,
    val identifiers: List<CaseIdentifier>,
    // plain data from the assessment port, so scoring stays pure (P17); `WITH_MODEL` scores as `confidence-3` (P22)
    val answers: Map<String, PremiseAnswers> = emptyMap(),
    val mode: AssessmentMode = AssessmentMode.RULES_ONLY,
)

/** The facts code computes about one Premise (D3). [identifierTypes] holds types only, never values. */
data class PremiseFacts(
    val authorIsPerson: AuthorMatch,
    val underProfile: Boolean,
    val identifierTypes: List<IdentifierType>,
    val identifierDistance: Int?,
    val conflictingEmail: Boolean,
    val addressCount: Int,
    val host: String?,
    val sourceName: String,
)

/** One cryptocurrency address in one Mention, with its assessment. Id: `"{mentionId}:{address}"`. */
data class Premise @JvmOverloads constructor(
    val id: String,
    val mentionId: String,
    val address: String,
    val window: AddressWindow,
    val facts: PremiseFacts,
    val status: PremiseStatus,
    val kind: PremiseKind?,
    val points: Int,
    val dismissalReason: DismissalReason?,
    // who assessed kind, declaration and warning: `MODEL` only when the model answered for this Premise (D16)
    val assessedBy: AssessedBy,
    // null when the model was off or gave no answer for this Premise
    val answers: PremiseAnswers? = null,
    // answers between the thresholds that still mattered for the status (P13)
    val undetermined: List<ModelQuestion> = emptyList(),
    // "Adres wycofany przez autora" (P14)
    val withdrawn: Boolean = false,
) {
    companion object {
        @JvmStatic
        fun idOf(
            mentionId: String,
            address: String,
        ): String = "$mentionId:$address"
    }
}

data class ScoredAttribution @JvmOverloads constructor(
    val candidate: Candidate,
    val confidenceLevel: Int,
    val appliedRules: List<AppliedRule>,
    // Analyses stored before algorithm confidence-1 have no Justification text
    val justification: String = "",
    val classificationStatus: ClassificationStatus = ClassificationStatus.NOT_EXCLUDED,
    // Analyses stored before algorithm confidence-2 have no Premises
    val premises: List<Premise> = emptyList(),
)

data class ScoringResult(
    val attributions: List<ScoredAttribution>,
    val algorithmVersion: String,
)

data class StageFailure(
    val stage: AnalysisStage,
    val detail: String,
)

data class AnalysisResult @JvmOverloads constructor(
    val status: AnalysisStatus,
    val failures: List<StageFailure> = emptyList(),
    val limitations: List<ReportLimitation> = emptyList(),
    val sources: List<SourceReport> = emptyList(),
    val mentions: List<FoundMention> = emptyList(),
    val rejected: List<RejectedCandidate> = emptyList(),
    val excluded: List<ExcludedCandidate> = emptyList(),
    val unclassified: List<Candidate> = emptyList(),
    val attributions: List<ScoredAttribution> = emptyList(),
    val algorithmVersion: String? = null,
    // Analyses stored before algorithm confidence-3 have no assessment audit fields
    val assessmentMode: AssessmentMode? = null,
    val modelVersion: String? = null,
    val questionSetVersion: String? = null,
)

data class AnalysisRun(
    val analysisId: String,
    val status: AnalysisStatus,
)

interface MentionSearch {
    fun search(identifiers: List<CaseIdentifier>): SearchResult
}

interface CandidatePreparation {
    fun prepare(mentions: List<FoundMention>): PreparedCandidates
}

interface ExclusionClassifier {
    fun classify(candidates: List<Candidate>): ExclusionResult
}

interface ConfidenceScoring {
    fun score(input: ScoringInput): ScoringResult
}

/**
 * "Assess these Premises": what does the text say about the address? Never "whose is it" (D4, D6).
 * An implementation never throws for a single Premise; it leaves that Premise out of the answers.
 */
interface PremiseAssessment {
    /** `RULES_ONLY` when the model is off: [assess] then answers nothing and no Report Limitation is added. */
    val mode: AssessmentMode

    fun assess(
        premises: List<PremiseToAssess>,
        identifiers: List<CaseIdentifier>,
    ): PremiseAssessments
}

interface AnalysisRecorder {
    fun begin(request: AnalysisRequest): String

    fun finish(
        analysisId: String,
        result: AnalysisResult,
    )
}
