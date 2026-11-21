package squad.nononotaa.backend.dto

import squad.nononotaa.backend.model.AddressActivityEventType
import squad.nononotaa.backend.model.AddressValidationFailure
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.AttributionChangeType
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ComparabilityLimitation
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.ScheduleAttemptOutcome
import squad.nononotaa.backend.model.SourceStatus
import java.math.BigDecimal
import java.time.Instant

data class TestResponse(
    val message: String,
)

data class PersonIdentifiersResponse(
    val nickname: String?,
    val login: String?,
    val email: String?,
    val messengerId: String?,
    val profileUrl: String?,
    val fullName: String?,
)

data class CaseResponse(
    val id: String,
    val identifiers: PersonIdentifiersResponse,
    val createdAt: Instant,
    val latestAnalysis: AnalysisResponse?,
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
)

// TODO E2.2: fill network, valid and validationFailure from AddressValidator, never from the detection alone
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
)

data class ScoringRuleContributionResponse(
    val rule: String,
    val contribution: Int,
)

data class AddressActivityEventResponse(
    val type: AddressActivityEventType,
    val transactionId: String,
    val occurredAt: Instant,
    val amount: BigDecimal?,
    val unit: String?,
)

data class AddressActivityResponse(
    val totalReceived: BigDecimal?,
    val totalSent: BigDecimal?,
    val unit: String?,
    val events: List<AddressActivityEventResponse>,
    val coverageLimitation: String?,
)

data class AttributionResponse(
    val address: String,
    val network: Network,
    val confidenceLevel: Int,
    val classificationStatus: ClassificationStatus,
    val justification: List<MentionResponse>,
    val scoringRules: List<ScoringRuleContributionResponse>,
    val addressActivity: AddressActivityResponse?,
)

data class ExcludedAddressResponse(
    val address: String,
    val network: Network,
    val reason: ExclusionReason,
    val classificationSource: String,
    val justification: List<MentionResponse>,
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
    val cadence: String?,
    val timezone: String?,
    val nextExecutionAt: Instant?,
    val lastAttempt: ScheduleAttemptResponse?,
)
