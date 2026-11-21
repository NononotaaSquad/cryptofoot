package squad.nononotaa.backend.controller

import groovy.json.JsonSlurper
import java.time.Instant
import java.time.ZoneId
import org.springframework.http.MediaType
import squad.nononotaa.backend.BaseSpec

class ScheduleApiSpec extends BaseSpec {

    String caseId = "case-${UUID.randomUUID()}"

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

    def "the demo cadence is refused unless demo mode is on, an unknown cadence is a 400"() {
        expect:
        put([enabled: true, cadence: 'DEMO_EVERY_MINUTE']).expectStatus().isBadRequest()
        .expectBody().jsonPath('$.message').isEqualTo('Tryb demonstracyjny harmonogramu jest wyłączony.')
        put([enabled: true, cadence: 'EVERY_SECOND']).expectStatus().isBadRequest()
        read().enabled == false
    }
}
