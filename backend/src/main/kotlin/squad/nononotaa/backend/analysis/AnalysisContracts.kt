package squad.nononotaa.backend.analysis

import squad.nononotaa.backend.model.AddressActivityCoverageLimitation
import squad.nononotaa.backend.model.AddressActivityStatus
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.CashOutSignalKind
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.SourceStatus
import java.math.BigDecimal
import java.time.Instant

enum class AnalysisStage { SEARCH, CANDIDATE_PREPARATION, EXCLUSION, SCORING, ADDRESS_ACTIVITY }

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

data class FoundMention @JvmOverloads constructor(
    val id: String,
    val sourceName: String,
    val url: String,
    val collectedAt: Instant,
    val context: String?,
    val detectedAddresses: List<String>,
    val author: String? = null,
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

data class AppliedRule(
    val rule: String,
    val contribution: Int,
)

data class ScoringInput(
    val accepted: List<Candidate>,
    val unclassified: List<Candidate>,
    val mentions: List<FoundMention>,
    val identifiers: List<CaseIdentifier>,
)

data class CashOutSignal(
    val kind: CashOutSignalKind,
    val transactionId: String,
    val transactionUrl: String?,
    val occurredAt: Instant,
    val amount: BigDecimal,
    val unit: String,
    val counterpartyAddress: String?,
    val counterpartyLabel: String?,
)

data class AddressActivity(
    val sourceName: String,
    val synthetic: Boolean,
    val checkedAt: Instant,
    // null: stored before the activity rules were versioned (D23-011)
    val rulesVersion: String? = null,
    val stablecoinSwapThreshold: BigDecimal? = null,
    val unit: String,
    val totalReceived: BigDecimal?,
    val totalSent: BigDecimal?,
    val transactionCount: Int?,
    val firstActivityAt: Instant?,
    val lastActivityAt: Instant?,
    val cashOutSignals: List<CashOutSignal>,
    val coverageLimitations: List<AddressActivityCoverageLimitation>,
)

data class AddressActivityOutcome(
    val status: AddressActivityStatus,
    val activity: AddressActivity? = null,
)

data class ScoredAttribution
@JvmOverloads
constructor(
    val candidate: Candidate,
    val confidenceLevel: Int,
    val appliedRules: List<AppliedRule>,
    // Analyses stored before algorithm confidence-1 have no Justification text
    val justification: String = "",
    val classificationStatus: ClassificationStatus = ClassificationStatus.NOT_EXCLUDED,
    // null: not checked (Confidence Level 0, or an Analysis stored before E4)
    val addressActivity: AddressActivityOutcome? = null,
)

data class ScoringResult(
    val attributions: List<ScoredAttribution>,
    val algorithmVersion: String,
)

data class StageFailure(
    val stage: AnalysisStage,
    val detail: String,
)

data class AnalysisResult(
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

interface AddressActivityCollector {
    // an Attribution missing from the map was not checked (D23-002)
    fun collect(attributions: List<ScoredAttribution>): Map<Candidate, AddressActivityOutcome>
}

interface AnalysisRecorder {
    fun begin(request: AnalysisRequest): String

    fun finish(
        analysisId: String,
        result: AnalysisResult,
    )
}
