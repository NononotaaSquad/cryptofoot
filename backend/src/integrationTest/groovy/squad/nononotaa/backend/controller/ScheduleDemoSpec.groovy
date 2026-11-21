package squad.nononotaa.backend.controller

import groovy.json.JsonSlurper
import java.time.Duration
import java.time.Instant
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import squad.nononotaa.backend.BaseSpec

@TestPropertySource(properties = 'app.scheduler.demo-enabled=true')
class ScheduleDemoSpec extends BaseSpec {

    def "the shortened demo interval is accepted only in demo mode and is labelled"() {
        given:
        def caseId = "case-${UUID.randomUUID()}"

        when:
        client.put().uri("/api/1/cases/${caseId}/schedule").contentType(MediaType.APPLICATION_JSON)
                .body([enabled: true, cadence: 'DEMO_EVERY_MINUTE']).exchange().expectStatus().isOk()
        def schedule = new JsonSlurper().parseText(client.get().uri("/api/1/cases/${caseId}/schedule").exchange()
                .expectBody(String).returnResult().responseBody) as Map

        then:
        schedule.cadence == 'DEMO_EVERY_MINUTE'
        schedule.demo == true
        Duration.between(Instant.now(), Instant.parse(schedule.nextExecutionAt as String)) <= Duration.ofMinutes(1)
    }
}
