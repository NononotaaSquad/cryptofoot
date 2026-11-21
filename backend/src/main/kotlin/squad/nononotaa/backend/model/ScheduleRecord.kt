package squad.nononotaa.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

data class ScheduleAttempt(
    val attemptedAt: Instant,
    val outcome: ScheduleAttemptOutcome,
    val analysisId: String? = null,
)

@Document("schedules")
data class ScheduleRecord(
    @Id val caseId: String,
    @Version val version: Long? = null,
    val enabled: Boolean,
    val cadence: SchedulePreset,
    val nextExecutionAt: Instant?,
    val lastAttempt: ScheduleAttempt? = null,
)
