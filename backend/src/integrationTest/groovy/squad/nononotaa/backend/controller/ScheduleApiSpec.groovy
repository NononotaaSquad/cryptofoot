package squad.nononotaa.backend.controller

import groovy.json.JsonSlurper
import java.time.Instant
import java.time.ZoneId
import org.springframework.http.MediaType
import squad.nononotaa.backend.BaseSpec

class ScheduleApiSpec extends BaseSpec {

    String caseId

    def setup() {
        caseId = createCase()
    }

    private put(Map body) {
        client.put().uri("/api/1/cases/${caseId}/schedule").contentType(MediaType.APPLICATION_JSON).body(body).exchange()
    }

    private Map read() {
        new JsonSlurper().parseText(client.get().uri("/api/1/cases/${caseId}/schedule").exchange()
                .expectStatus().isOk().expectBody(String).returnResult().responseBody) as Map
    }

    def "a case without a schedule is disabled and shows the timezone"() {
        expect:
        client.get().uri("/api/1/cases/${caseId}/schedule").exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath('$.enabled').isEqualTo(false)
                .jsonPath('$.cadence').isEmpty()
                .jsonPath('$.timezone').isEqualTo('Europe/Warsaw')
                .jsonPath('$.nextExecutionAt').isEmpty()
                .jsonPath('$.lastAttempt').isEmpty()
                .jsonPath('$.demo').isEqualTo(false)
    }

    def "enabling uses the daily 06:00 Warsaw default and shows the next execution"() {
        when:
        put([enabled: true]).expectStatus().isOk()
        def schedule = read()
        def next = Instant.parse(schedule.nextExecutionAt as String).atZone(ZoneId.of('Europe/Warsaw'))

        then:
        schedule.enabled == true
        schedule.cadence == 'DAILY'
        schedule.timezone == 'Europe/Warsaw'
        next.hour == 6
        next.minute == 0
        next.toInstant().isAfter(Instant.now())
    }

    def "disabling clears the next execution and keeps the cadence"() {
        given:
        put([enabled: true, cadence: 'WEEKLY']).expectStatus().isOk()

        when:
        put([enabled: false]).expectStatus().isOk()

        then:
        def schedule = read()
        schedule.enabled == false
        schedule.cadence == 'WEEKLY'
        schedule.nextExecutionAt == null
    }

    def "changing the cadence recomputes the next execution, repeating it keeps it"() {
        given:
        put([enabled: true, cadence: 'DAILY']).expectStatus().isOk()
        def daily = read().nextExecutionAt

        when:
        put([enabled: true, cadence: 'DAILY']).expectStatus().isOk()
        def repeated = read().nextExecutionAt
        put([enabled: true, cadence: 'WEEKLY']).expectStatus().isOk()
        def weekly = Instant.parse(read().nextExecutionAt as String).atZone(ZoneId.of('Europe/Warsaw'))

        then:
        repeated == daily
        weekly.dayOfWeek.name() == 'MONDAY'
        weekly.hour == 6
    }

    def "a schedule can only be set for an existing Case and nothing is stored otherwise"() {
        given:
        def unknown = "unknown-${UUID.randomUUID()}"

        expect:
        client.put().uri("/api/1/cases/${unknown}/schedule").contentType(MediaType.APPLICATION_JSON).body([enabled: true]).exchange()
        .expectStatus().isNotFound()
        .expectBody().jsonPath('$.message').isEqualTo('Nie znaleziono sprawy o podanym identyfikatorze.')
        client.get().uri("/api/1/cases/${unknown}/schedule").exchange()
                .expectStatus().isOk().expectBody().jsonPath('$.enabled').isEqualTo(false)
    }

    def "the demo cadence is refused unless demo mode is on, an unknown cadence is a 400"() {
        expect:
        put([enabled: true, cadence: 'DEMO_EVERY_MINUTE']).expectStatus().isBadRequest()
        .expectBody().jsonPath('$.message').isEqualTo('Tryb demonstracyjny harmonogramu jest wyłączony.')
        put([enabled: true, cadence: 'EVERY_SECOND']).expectStatus().isBadRequest()
        read().enabled == false
    }

    private custom(Object value, Object unit, boolean enabled = true) {
        put([enabled: enabled, cadence: 'CUSTOM_INTERVAL', intervalValue: value, intervalUnit: unit])
    }

    def "a custom interval is saved, read back and first due one interval from now"() {
        given:
        def before = Instant.now()

        when:
        custom(1, 'MINUTES').expectStatus().isOk()
                .expectBody().jsonPath('$.cadence').isEqualTo('CUSTOM_INTERVAL')
                .jsonPath('$.intervalValue').isEqualTo(1)
                .jsonPath('$.intervalUnit').isEqualTo('MINUTES')
                .jsonPath('$.demo').isEqualTo(false)
        def schedule = read()
        def next = Instant.parse(schedule.nextExecutionAt as String)

        then:
        schedule.enabled == true
        schedule.cadence == 'CUSTOM_INTERVAL'
        schedule.intervalValue == 1
        schedule.intervalUnit == 'MINUTES'
        schedule.timezone == 'Europe/Warsaw'
        next.isAfter(before.plusSeconds(55))
        next.isBefore(Instant.now().plusSeconds(65))
    }

    def "the custom interval is accepted at the edges of the allowed range"() {
        expect:
        custom(value, unit).expectStatus().isOk()
        read().intervalValue == value

        where:
        value  | unit
        1      | 'MINUTES'
        525600 | 'MINUTES'
        8760   | 'HOURS'
        365    | 'DAYS'
    }

    def "the custom interval is refused outside the range, with a Polish message"() {
        expect:
        custom(value, unit).expectStatus().isBadRequest()
        read().enabled == false

        where:
        value               | unit
        0                   | 'MINUTES'
        -5                  | 'MINUTES'
        1.5                 | 'MINUTES'
        525601              | 'MINUTES'
        8761                | 'HOURS'
        366                 | 'DAYS'
        999999999999999999L | 'DAYS'
        1                   | 'SECONDS'
        '1'                 | 'MINUTES'
        null                | 'MINUTES'
        5                   | null
    }

    def "an invalid interval says what is wrong and does not touch a saved schedule"() {
        given:
        custom(30, 'MINUTES').expectStatus().isOk()
        def saved = read()

        expect:
        custom(0, 'MINUTES').expectStatus().isBadRequest()
                .expectBody().jsonPath('$.message').isEqualTo('Interwał musi wynosić od 1 minuty do 365 dni.')
        custom(null, null).expectStatus().isBadRequest()
                .expectBody().jsonPath('$.message').isEqualTo('Podaj liczbę i jednostkę własnego interwału.')
        read() == saved
    }

    def "changing the interval recomputes the next term, repeating it keeps it"() {
        given:
        custom(10, 'MINUTES').expectStatus().isOk()
        def first = read().nextExecutionAt

        when:
        custom(10, 'MINUTES').expectStatus().isOk()
        def repeated = read().nextExecutionAt
        custom(2, 'HOURS').expectStatus().isOk()
        def changed = read()

        then:
        repeated == first
        changed.nextExecutionAt != first
        Instant.parse(changed.nextExecutionAt as String).isAfter(Instant.now().plusSeconds(60 * 119))
        changed.intervalValue == 2
        changed.intervalUnit == 'HOURS'
    }

    def "disabling keeps the interval and clears the next term, enabling again reuses it"() {
        given:
        custom(3, 'DAYS').expectStatus().isOk()

        when:
        put([enabled: false]).expectStatus().isOk()
        def off = read()
        put([enabled: true]).expectStatus().isOk()
        def on = read()

        then:
        off.enabled == false
        off.nextExecutionAt == null
        off.intervalValue == 3
        on.enabled == true
        on.cadence == 'CUSTOM_INTERVAL'
        on.intervalValue == 3
        on.intervalUnit == 'DAYS'
        Instant.parse(on.nextExecutionAt as String).isAfter(Instant.now().plusSeconds(60 * 60 * 24 * 3 - 60))
    }

    def "switching to a preset drops the custom interval"() {
        given:
        custom(5, 'MINUTES').expectStatus().isOk()

        when:
        put([enabled: true, cadence: 'WEEKLY']).expectStatus().isOk()
        def schedule = read()

        then:
        schedule.cadence == 'WEEKLY'
        schedule.intervalValue == null
        schedule.intervalUnit == null
    }
}
