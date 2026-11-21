package squad.nononotaa.backend

import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class OpenApiContractSpec extends BaseSpec {

    static final JsonGenerator JSON = new JsonGenerator.Options().disableUnicodeEscaping().build()

    static final File SNAPSHOT = new File(System.getProperty('openapi.snapshot'))

    def "frontend openapi.json matches the backend contract"() {
        given:
        def body = client.get().uri('/v3/api-docs').exchange()
                .expectStatus().isOk()
                .expectBody(String).returnResult().responseBody
        def actual = withoutServers(new JsonSlurper().parseText(body) as Map)
        if (Boolean.getBoolean('openapi.update')) {
            SNAPSHOT.setText(JsonOutput.prettyPrint(JSON.toJson(actual), true) + '\n', 'UTF-8')
        }

        expect:
        assert actual == withoutServers(new JsonSlurper().parse(SNAPSHOT, 'UTF-8') as Map):
        'frontend/apps/web/lib/api/openapi.json is out of date: run ./scripts/api-sync.sh and commit the result'
    }

    // the server URL depends on where the spec was fetched (random test port, local curl)
    private static Map withoutServers(Map spec) {
        spec.findAll { it.key != 'servers' }
    }
}
