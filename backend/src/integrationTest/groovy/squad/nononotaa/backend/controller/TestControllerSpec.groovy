package squad.nononotaa.backend.controller

import org.springframework.http.MediaType
import squad.nononotaa.backend.BaseSpec

class TestControllerSpec extends BaseSpec {

    def "GET returns greeting"() {
        expect:
        client.get().uri('/api/1/test').exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.message').isEqualTo('Hello')
    }

    def "POST greets by name"() {
        expect:
        client.post().uri('/api/1/test')
                .contentType(MediaType.APPLICATION_JSON)
                .body([name: 'Ada'])
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.message').isEqualTo('Hello, Ada')
    }

    def "POST rejects #description with a Polish field error"() {
        expect:
        client.post().uri('/api/1/test')
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.code').isEqualTo(400)
                .jsonPath('$.message').isEqualTo('Popraw błędy w formularzu.')
                .jsonPath('$.errors[0].field').isEqualTo('name')
                .jsonPath('$.errors[0].message').isEqualTo('To pole jest wymagane.')

        where:
        description    | body
        'blank name'   | [name: ' ']
        'missing name' | [:]
        'null name'    | [name: null]
    }
}
