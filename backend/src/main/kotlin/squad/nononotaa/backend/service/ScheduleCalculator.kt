package squad.nononotaa.backend.service

import org.springframework.stereotype.Component
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.model.ScheduleRecord
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Component
class ScheduleCalculator {
    fun first(
        preset: SchedulePreset,
        now: Instant,
        interval: Duration?,
    ): Instant = if (preset == SchedulePreset.CUSTOM_INTERVAL) now.plus(requireNotNull(interval)) else next(preset, now)

    // A custom interval keeps its grid anchored on the claimed term, so a late tick does not drift later terms.
    fun following(
        schedule: ScheduleRecord,
        due: Instant,
        now: Instant,
    ): Instant {
        val interval = schedule.interval ?: return next(schedule.cadence, now)
        val elapsed = Duration.between(due, now)
        val steps = if (elapsed.isNegative) 1L else elapsed.dividedBy(interval) + 1
        return due.plus(interval.multipliedBy(steps))
    }

    fun next(
        preset: SchedulePreset,
        after: Instant,
    ): Instant = when (preset) {
        SchedulePreset.CUSTOM_INTERVAL -> throw IllegalArgumentException("A custom interval needs its own length")
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
