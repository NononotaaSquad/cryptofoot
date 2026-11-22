package squad.nononotaa.backend.service

import java.time.Duration
import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.model.ScheduleIntervalUnit
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.model.ScheduleRecord

class ScheduleCalculatorSpec extends Specification {

    def calculator = new ScheduleCalculator()

    def "#preset after #after is #expected"() {
        expect:
        calculator.next(preset, Instant.parse(after)) == Instant.parse(expected)

        where:
        preset                            | after                  | expected
        SchedulePreset.DAILY              | '2026-11-21T04:00:00Z' | '2026-11-21T05:00:00Z'
        SchedulePreset.DAILY              | '2026-11-21T05:00:00Z' | '2026-11-22T05:00:00Z'
        SchedulePreset.DAILY              | '2026-07-01T03:00:00Z' | '2026-07-01T04:00:00Z'
        SchedulePreset.DAILY              | '2026-03-28T05:00:00Z' | '2026-03-29T04:00:00Z'
        SchedulePreset.DAILY              | '2026-10-24T04:00:00Z' | '2026-10-25T05:00:00Z'
        SchedulePreset.EVERY_6_HOURS      | '2026-11-21T06:30:00Z' | '2026-11-21T11:00:00Z'
        SchedulePreset.EVERY_6_HOURS      | '2026-11-21T22:30:00Z' | '2026-11-21T23:00:00Z'
        SchedulePreset.WEEKLY             | '2026-11-21T10:00:00Z' | '2026-11-23T05:00:00Z'
        SchedulePreset.WEEKLY             | '2026-11-23T05:00:00Z' | '2026-11-30T05:00:00Z'
        SchedulePreset.DEMO_EVERY_MINUTE  | '2026-11-21T10:00:30Z' | '2026-11-21T10:01:00Z'
        SchedulePreset.DEMO_EVERY_MINUTE  | '2026-11-21T10:00:00Z' | '2026-11-21T10:01:00Z'
    }

    def "only the demo preset is marked as demo"() {
        expect:
        SchedulePreset.values().findAll {
            it.demo
        } == [
            SchedulePreset.DEMO_EVERY_MINUTE
        ]
    }

    private static ScheduleRecord custom(long value, ScheduleIntervalUnit unit) {
        new ScheduleRecord('c', null, true, SchedulePreset.CUSTOM_INTERVAL, value, unit, null, null)
    }

    def "the first term of a custom interval is one full interval after now, not rounded to the clock"() {
        expect:
        calculator.first(SchedulePreset.CUSTOM_INTERVAL, Instant.parse('2026-11-21T10:00:20Z'), Duration.ofMinutes(1)) == Instant.parse('2026-11-21T10:01:20Z')
        calculator.first(SchedulePreset.CUSTOM_INTERVAL, Instant.parse('2026-11-21T10:00:20Z'), Duration.ofDays(2)) == Instant.parse('2026-11-23T10:00:20Z')
        calculator.first(SchedulePreset.DAILY, Instant.parse('2026-11-21T04:00:00Z'), null) == Instant.parse('2026-11-21T05:00:00Z')
    }

    def "a custom interval stays on the grid of the claimed term when the tick is late"() {
        expect:
        calculator.following(custom(value, unit), Instant.parse(due), Instant.parse(now)) == Instant.parse(expected)

        where:
        value | unit                         | due                    | now                    | expected
        1     | ScheduleIntervalUnit.MINUTES | '2026-11-21T10:01:20Z' | '2026-11-21T10:01:27Z' | '2026-11-21T10:02:20Z'
        1     | ScheduleIntervalUnit.MINUTES | '2026-11-21T10:01:20Z' | '2026-11-21T10:02:20Z' | '2026-11-21T10:03:20Z'
        1     | ScheduleIntervalUnit.MINUTES | '2026-11-21T10:01:20Z' | '2026-11-21T10:05:00Z' | '2026-11-21T10:05:20Z'
        90    | ScheduleIntervalUnit.MINUTES | '2026-11-21T10:00:00Z' | '2026-11-21T10:00:01Z' | '2026-11-21T11:30:00Z'
        2     | ScheduleIntervalUnit.HOURS   | '2026-11-21T10:00:00Z' | '2026-11-21T10:00:01Z' | '2026-11-21T12:00:00Z'
        1     | ScheduleIntervalUnit.DAYS    | '2026-11-21T10:00:00Z' | '2026-11-24T09:00:00Z' | '2026-11-24T10:00:00Z'
    }

    def "an early claim never moves the next term into the past"() {
        expect:
        calculator.following(custom(1, ScheduleIntervalUnit.MINUTES), Instant.parse('2026-11-21T10:01:00Z'), Instant.parse('2026-11-21T10:00:00Z')) == Instant.parse('2026-11-21T10:02:00Z')
    }

    def "presets without a custom length follow the calendar after the claim"() {
        expect:
        calculator.following(new ScheduleRecord('c', null, true, SchedulePreset.DAILY, null, null, null, null), Instant.parse('2026-11-21T05:00:00Z'), Instant.parse('2026-11-21T05:00:10Z')) == Instant.parse('2026-11-22T05:00:00Z')
    }
}
