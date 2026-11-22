package squad.nononotaa.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Duration
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
    val intervalValue: Long? = null,
    val intervalUnit: ScheduleIntervalUnit? = null,
    val nextExecutionAt: Instant?,
    val lastAttempt: ScheduleAttempt? = null,
) {
    val interval: Duration?
        get() = if (cadence == SchedulePreset.CUSTOM_INTERVAL && intervalValue != null && intervalUnit != null) {
            Duration.of(intervalValue, intervalUnit.unit)
        } else {
            null
        }
}
