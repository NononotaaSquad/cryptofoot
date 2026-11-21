package squad.nononotaa.backend.service

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import squad.nononotaa.backend.config.SchedulerProperties
import squad.nononotaa.backend.exception.ConflictException
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.ScheduleAttempt
import squad.nononotaa.backend.model.ScheduleAttemptOutcome
import squad.nononotaa.backend.model.ScheduleRecord
import squad.nononotaa.backend.repository.ScheduleRepository
import squad.nononotaa.backend.util.logger
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

@Component
@ConditionalOnProperty("app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class ScheduledRunner(
    private val repository: ScheduleRepository,
    private val rerun: ObjectProvider<AnalysisRerunService>,
    private val calculator: ScheduleCalculator,
    private val clock: Clock,
    private val properties: SchedulerProperties,
) {
    private val idleWarned = AtomicBoolean(false)

    @Scheduled(fixedDelayString = "\${app.scheduler.tick:PT30S}")
    fun runDue() {
        val analyses = rerun.getIfAvailable()
        if (analyses == null) {
            if (idleWarned.compareAndSet(false, true)) logger.warn("Scheduler idle: the Analysis flow is not wired")
            return
        }
        val now = clock.instant()
        repository.findByEnabledTrueAndNextExecutionAtLessThanEqual(now).forEach { schedule ->
            try {
                process(schedule, now, analyses)
            } catch (e: OptimisticLockingFailureException) {
                logger.warn("Schedule claimed elsewhere case={}", schedule.caseId)
            } catch (e: Exception) {
                logger.error("Schedule processing failed case={}", schedule.caseId, e)
            }
        }
    }

    private fun process(
        schedule: ScheduleRecord,
        now: Instant,
        analyses: AnalysisRerunService,
    ) {
        val due = requireNotNull(schedule.nextExecutionAt) { "Enabled schedule without next execution" }
        val claimed = repository.save(schedule.copy(nextExecutionAt = calculator.next(schedule.cadence, now)))
        val attempt =
            if (now.isAfter(due.plus(properties.grace))) {
                logger.warn("Scheduled run missed during downtime case={} due={}", schedule.caseId, due)
                ScheduleAttempt(now, ScheduleAttemptOutcome.SKIPPED_DOWNTIME)
            } else {
                attempt(schedule.caseId, now, analyses)
            }
        repository.save(claimed.copy(lastAttempt = attempt))
        logger.info("Schedule attempt case={} outcome={} next={}", schedule.caseId, attempt.outcome, claimed.nextExecutionAt)
    }

    private fun attempt(
        caseId: String,
        now: Instant,
        analyses: AnalysisRerunService,
    ): ScheduleAttempt = try {
        ScheduleAttempt(now, ScheduleAttemptOutcome.STARTED, analyses.rerun(caseId, AnalysisTrigger.SCHEDULED).id)
    } catch (e: ConflictException) {
        logger.warn("Scheduled run skipped: an analysis is already running case={}", caseId)
        ScheduleAttempt(now, ScheduleAttemptOutcome.SKIPPED_OVERLAP)
    } catch (e: Exception) {
        logger.error("Scheduled run failed case={}", caseId, e)
        ScheduleAttempt(now, ScheduleAttemptOutcome.FAILED)
    }

    companion object {
        private val logger = logger()
    }
}
