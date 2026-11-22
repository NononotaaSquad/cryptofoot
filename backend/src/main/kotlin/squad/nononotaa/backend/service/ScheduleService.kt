package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.config.SchedulerProperties
import squad.nononotaa.backend.dto.ScheduleAttemptResponse
import squad.nononotaa.backend.dto.ScheduleResponse
import squad.nononotaa.backend.dto.UpdateScheduleRequest
import squad.nononotaa.backend.exception.BadRequestException
import squad.nononotaa.backend.exception.NotFoundException
import squad.nononotaa.backend.model.ScheduleIntervalUnit
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.model.ScheduleRecord
import squad.nononotaa.backend.repository.CaseRepository
import squad.nononotaa.backend.repository.ScheduleRepository
import squad.nononotaa.backend.util.logger
import java.time.Clock
import java.time.Duration

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
            ScheduleResponse(false, null, null, null, false, ScheduleCalculator.ZONE.id, null, null)
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
        val (value, unit) = resolveInterval(caseId, request, existing, cadence)
        val candidate = ScheduleRecord(caseId, existing?.version, request.enabled, cadence, value, unit, null)
        val unchanged =
            existing?.enabled == true && existing.cadence == cadence && existing.intervalValue == value && existing.intervalUnit == unit
        val next =
            when {
                !request.enabled -> null
                unchanged -> existing.nextExecutionAt
                else -> calculator.first(cadence, clock.instant(), candidate.interval)
            }
        val saved =
            repository.save(
                (existing ?: candidate).copy(
                    enabled = request.enabled,
                    cadence = cadence,
                    intervalValue = value,
                    intervalUnit = unit,
                    nextExecutionAt = next,
                ),
            )
        logger.info(
            "Schedule updated case={} enabled={} cadence={} interval={}{} next={}",
            caseId,
            saved.enabled,
            saved.cadence,
            saved.intervalValue,
            saved.intervalUnit,
            saved.nextExecutionAt,
        )
        return toResponse(saved)
    }

    private fun resolveInterval(
        caseId: String,
        request: UpdateScheduleRequest,
        existing: ScheduleRecord?,
        cadence: SchedulePreset,
    ): Pair<Long?, ScheduleIntervalUnit?> {
        if (cadence != SchedulePreset.CUSTOM_INTERVAL) return null to null
        val inherits = request.cadence == null && request.intervalValue == null && request.intervalUnit == null
        if (inherits && existing != null) return existing.intervalValue to existing.intervalUnit
        val value = request.intervalValue
        val unit = request.intervalUnit
        if (value == null || unit == null) rejectInterval(caseId, "Podaj liczb\u0119 i jednostk\u0119 w\u0142asnego interwa\u0142u.")
        if (value < 1 || Duration.of(value.coerceAtMost(MAX_VALUE), unit.unit) !in MIN_INTERVAL..MAX_INTERVAL) {
            rejectInterval(caseId, "Interwa\u0142 musi wynosi\u0107 od 1 minuty do 365 dni.")
        }
        return value to unit
    }

    private fun rejectInterval(
        caseId: String,
        message: String,
    ): Nothing {
        logger.warn("Schedule rejected: invalid interval case={}", caseId)
        throw BadRequestException(message)
    }

    private fun toResponse(record: ScheduleRecord) = ScheduleResponse(
        enabled = record.enabled,
        cadence = record.cadence,
        intervalValue = record.intervalValue,
        intervalUnit = record.intervalUnit,
        demo = record.cadence.demo,
        timezone = ScheduleCalculator.ZONE.id,
        nextExecutionAt = record.nextExecutionAt,
        lastAttempt = record.lastAttempt?.let { ScheduleAttemptResponse(it.attemptedAt, it.outcome, it.analysisId) },
    )

    companion object {
        private val logger = logger()
        private val MIN_INTERVAL = Duration.ofMinutes(1)
        private val MAX_INTERVAL = Duration.ofDays(365)
        private const val MAX_VALUE = 1_000_000L
    }
}
