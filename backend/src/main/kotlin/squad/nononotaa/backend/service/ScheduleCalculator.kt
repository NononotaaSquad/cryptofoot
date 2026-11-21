package squad.nononotaa.backend.service

import org.springframework.stereotype.Component
import squad.nononotaa.backend.model.SchedulePreset
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Component
class ScheduleCalculator {
    fun next(
        preset: SchedulePreset,
        after: Instant,
    ): Instant = when (preset) {
        SchedulePreset.DAILY -> nextLocal(after, listOf(6), null)
        SchedulePreset.EVERY_6_HOURS -> nextLocal(after, listOf(0, 6, 12, 18), null)
        SchedulePreset.WEEKLY -> nextLocal(after, listOf(6), DayOfWeek.MONDAY)
        SchedulePreset.DEMO_EVERY_MINUTE -> after.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES)
    }

    private fun nextLocal(
        after: Instant,
        hours: List<Int>,
        dayOfWeek: DayOfWeek?,
    ): Instant {
        val today = after.atZone(ZONE).toLocalDate()
        return (0L..7L)
            .map { today.plusDays(it) }
            .filter { dayOfWeek == null || it.dayOfWeek == dayOfWeek }
            .flatMap { date -> hours.map { ZonedDateTime.of(date, LocalTime.of(it, 0), ZONE).toInstant() } }
            .first { it.isAfter(after) }
    }

    companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Warsaw")
    }
}
