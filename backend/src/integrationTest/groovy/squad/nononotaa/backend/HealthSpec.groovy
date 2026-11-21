package squad.nononotaa.backend

class HealthSpec extends BaseSpec {

    def "reports #path as UP"() {
        expect:
        client.get().uri(path).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.status').isEqualTo('UP')

        where:
        path << [
            '/actuator/health',
            '/actuator/health/liveness',
            '/actuator/health/readiness'
        ]
    }
}
