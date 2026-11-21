package squad.nononotaa.backend.service

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.model.SchedulePreset

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
}
