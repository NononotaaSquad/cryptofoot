package squad.nononotaa.backend

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.client.RestTestClient
import org.testcontainers.mongodb.MongoDBContainer
import spock.lang.Specification

// CI must not reach the public internet; specs of the real sources enable them against a local stub
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    'sources.github.enabled=false',
    'sources.bitcointalk.enabled=false'
])
@AutoConfigureRestTestClient
abstract class BaseSpec extends Specification {

    static final MongoDBContainer MONGODB = new MongoDBContainer('mongo:8')

    static {
        MONGODB.start()
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add('MONGODB_URI') { MONGODB.getReplicaSetUrl('test') }
        // specs drive ScheduledRunner with their own clock; the background tick would race them
        registry.add('app.scheduler.enabled') { 'false' }
    }

    @Autowired
    protected RestTestClient client

    protected String createCase(Map identifiers = [nickname: "nick-${UUID.randomUUID()}".toString()]) {
        def body = client.post().uri('/api/1/cases').contentType(MediaType.APPLICATION_JSON).body(identifiers).exchange()
                .expectStatus().isCreated().expectBody(String).returnResult().responseBody
        new JsonSlurper().parseText(body).id as String
    }
}
