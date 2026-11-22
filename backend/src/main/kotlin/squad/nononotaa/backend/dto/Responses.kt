package squad.nononotaa.backend.dto

import squad.nononotaa.backend.model.AddressActivityCoverageLimitation
import squad.nononotaa.backend.model.AddressActivityStatus
import squad.nononotaa.backend.model.AddressValidationFailure
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.AttributionChangeType
import squad.nononotaa.backend.model.CashOutSignalKind
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ComparabilityLimitation
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.ScheduleAttemptOutcome
import squad.nononotaa.backend.model.ScheduleIntervalUnit
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.model.SourceStatus
import java.math.BigDecimal
import java.time.Instant

data class PersonIdentifiersResponse(
    val nickname: String?,
    val login: String?,
    val email: String?,
    val messengerId: String?,
    val profileUrl: String?,
    val fullName: String?,
    val walletAddress: String?,
)

data class CaseResponse(
    val id: String,
    val identifiers: PersonIdentifiersResponse,
    val createdAt: Instant,
    val latestAnalysis: AnalysisResponse?,
    val latestFinishedAnalysis: AnalysisResponse?,
)

data class SourceCoverageResponse(
    val sourceName: String,
    val status: SourceStatus,
    val synthetic: Boolean,
    val mentionCount: Int,
    val checkedAt: Instant?,
)

data class AnalysisResponse(
    val id: String,
    val caseId: String,
    val trigger: AnalysisTrigger,
    val status: AnalysisStatus,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val identifiers: PersonIdentifiersResponse,
    val sources: List<SourceCoverageResponse>,
    val networks: List<Network>,
    val algorithmVersion: String?,
    val attributionCount: Int?,
    val excludedAddressCount: Int?,
    val highestConfidenceLevel: Int?,
)

data class DetectedAddressResponse(
    val address: String,
    val network: Network?,
    val valid: Boolean,
    val validationFailure: AddressValidationFailure?,
)

data class MentionResponse(
    val id: String,
    val sourceName: String,
    val url: String,
    val collectedAt: Instant,
    val context: String?,
    val detectedAddresses: List<DetectedAddressResponse>,
    val author: String?,
)

data class ScoringRuleContributionResponse(
    val rule: String,
    val contribution: Int,
)

data class CashOutSignalResponse(
    val kind: CashOutSignalKind,
    val transactionId: String,
    val transactionUrl: String?,
    val occurredAt: Instant,
    val amount: BigDecimal,
    val unit: String,
    val counterpartyAddress: String?,
    val counterpartyLabel: String?,
)

data class AddressActivityResponse(
    val sourceName: String,
    val synthetic: Boolean,
    val checkedAt: Instant,
    val rulesVersion: String?,
    val stablecoinSwapThreshold: BigDecimal?,
    val unit: String,
    val totalReceived: BigDecimal?,
    val totalSent: BigDecimal?,
    val transactionCount: Int?,
    val firstActivityAt: Instant?,
    val lastActivityAt: Instant?,
    val cashOutSignals: List<CashOutSignalResponse>,
    val coverageLimitations: List<AddressActivityCoverageLimitation>,
)

data class AttributionResponse(
    val address: String,
    val network: Network,
    val confidenceLevel: Int,
    val classificationStatus: ClassificationStatus,
    val justification: String,
    val mentions: List<MentionResponse>,
    val scoringRules: List<ScoringRuleContributionResponse>,
    val addressActivityStatus: AddressActivityStatus,
    val addressActivity: AddressActivityResponse?,
)

data class ExcludedAddressResponse(
    val address: String,
    val network: Network,
    val reason: ExclusionReason,
    val classificationSource: String,
    val mentions: List<MentionResponse>,
)

data class ReportResponse(
    val analysis: AnalysisResponse,
    val attributions: List<AttributionResponse>,
    val excludedAddresses: List<ExcludedAddressResponse>,
    val limitations: List<ReportLimitation>,
)

data class AttributionChangeResponse(
    val address: String,
    val network: Network,
    val change: AttributionChangeType,
    val earlierConfidenceLevel: Int?,
    val laterConfidenceLevel: Int?,
    val earlierExclusionReason: ExclusionReason?,
    val laterExclusionReason: ExclusionReason?,
    val earlierMentionIds: List<String>,
    val laterMentionIds: List<String>,
    val absenceVerifiable: Boolean?,
)

data class AnalysisComparisonResponse(
    val earlierAnalysis: AnalysisResponse,
    val laterAnalysis: AnalysisResponse,
    val changes: List<AttributionChangeResponse>,
    val comparabilityLimitations: List<ComparabilityLimitation>,
)

data class ScheduleAttemptResponse(
    val attemptedAt: Instant,
    val outcome: ScheduleAttemptOutcome,
    val analysisId: String?,
)

data class ScheduleResponse(
    val enabled: Boolean,
    val cadence: SchedulePreset?,
    val intervalValue: Long?,
    val intervalUnit: ScheduleIntervalUnit?,
    val demo: Boolean,
    val timezone: String,
    val nextExecutionAt: Instant?,
    val lastAttempt: ScheduleAttemptResponse?,
)
