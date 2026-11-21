package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.config.SchedulerProperties
import squad.nononotaa.backend.dto.ScheduleAttemptResponse
import squad.nononotaa.backend.dto.ScheduleResponse
import squad.nononotaa.backend.dto.UpdateScheduleRequest
import squad.nononotaa.backend.exception.BadRequestException
import squad.nononotaa.backend.exception.NotFoundException
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.model.ScheduleRecord
import squad.nononotaa.backend.repository.CaseRepository
import squad.nononotaa.backend.repository.ScheduleRepository
import squad.nononotaa.backend.util.logger
import java.time.Clock

@Service
class ScheduleService(
    private val repository: ScheduleRepository,
    private val calculator: ScheduleCalculator,
    private val clock: Clock,
    private val properties: SchedulerProperties,
    private val cases: CaseRepository,
) {
    fun get(caseId: String): ScheduleResponse {
        logger.debug("Schedule read case={}", caseId)
        return repository.findById(caseId).map(::toResponse).orElseGet {
            ScheduleResponse(false, null, false, ScheduleCalculator.ZONE.id, null, null)
        }
    }

    fun update(
        caseId: String,
        request: UpdateScheduleRequest,
    ): ScheduleResponse {
        if (!cases.existsById(caseId)) {
            logger.warn("Schedule rejected: case not found case={}", caseId)
            throw NotFoundException("Nie znaleziono sprawy o podanym identyfikatorze.")
        }
        val existing = repository.findById(caseId).orElse(null)
        val cadence = request.cadence ?: existing?.cadence ?: SchedulePreset.DAILY
        if (request.enabled && cadence.demo && !properties.demoEnabled) {
            logger.warn("Schedule rejected: demo cadence is disabled case={}", caseId)
            throw BadRequestException("Tryb demonstracyjny harmonogramu jest wyłączony.")
        }
        val next =
            when {
                !request.enabled -> null
                existing?.enabled == true && existing.cadence == cadence -> existing.nextExecutionAt
                else -> calculator.next(cadence, clock.instant())
            }
        val saved =
            repository.save(
                (existing ?: ScheduleRecord(caseId = caseId, enabled = request.enabled, cadence = cadence, nextExecutionAt = next))
                    .copy(enabled = request.enabled, cadence = cadence, nextExecutionAt = next),
            )
        logger.info("Schedule updated case={} enabled={} cadence={} next={}", caseId, saved.enabled, saved.cadence, saved.nextExecutionAt)
        return toResponse(saved)
    }

    private fun toResponse(record: ScheduleRecord) = ScheduleResponse(
        enabled = record.enabled,
        cadence = record.cadence,
        demo = record.cadence.demo,
        timezone = ScheduleCalculator.ZONE.id,
        nextExecutionAt = record.nextExecutionAt,
        lastAttempt = record.lastAttempt?.let { ScheduleAttemptResponse(it.attemptedAt, it.outcome, it.analysisId) },
    )

    companion object {
        private val logger = logger()
    }
}
