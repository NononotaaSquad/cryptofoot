package squad.nononotaa.backend.exception

import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.testsupport.ThrowingController

@Import(ThrowingController)
class RestExceptionHandlerSpec extends BaseSpec {

    def "maps #path to #code with message '#message'"() {
        expect:
        client.get().uri(path).exchange()
                .expectStatus().isEqualTo(code)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath('$.code').isEqualTo(code)
                .jsonPath('$.message').isEqualTo(message)
                .jsonPath('$.errors').isEmpty()

        where:
        path                         | code | message
        '/api/1/errors/bad-request'  | 400  | 'bad input'
        '/api/1/errors/not-found'    | 404  | 'missing item'
        '/api/1/errors/conflict'     | 409  | 'duplicate item'
        '/api/1/errors/duplicate'    | 409  | 'Taki zasób już istnieje.'
        '/api/1/errors/validation'   | 422  | 'invalid item'
        '/api/1/errors/unexpected'   | 500  | 'Internal Server Error'
        '/api/2/errors/not-found'    | 400  | 'Nieprawidłowe żądanie.'
    }

    def "returns 404 in Polish for unknown route"() {
        expect:
        client.get().uri('/api/test').exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath('$.code').isEqualTo(404)
                .jsonPath('$.message').isEqualTo('Nie znaleziono zasobu.')
    }

    def "returns 405 in Polish for wrong http method"() {
        expect:
        client.post().uri('/api/1/errors/not-found').exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().exists('Allow')
                .expectBody()
                .jsonPath('$.code').isEqualTo(405)
                .jsonPath('$.message').isEqualTo('Ta operacja nie jest dostępna dla tego zasobu.')
    }

    def "returns 400 without parser details for malformed body"() {
        expect:
        client.post().uri('/api/1/errors/body')
                .contentType(MediaType.APPLICATION_JSON)
                .body('{not json')
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.code').isEqualTo(400)
                .jsonPath('$.message').isEqualTo('Nieprawidłowe żądanie.')
                .jsonPath('$.errors').isEmpty()
    }

    def "lists every invalid field of a request body"() {
        expect:
        client.post().uri('/api/1/errors/contact')
                .contentType(MediaType.APPLICATION_JSON)
                .body([name: '', email: 'not-an-email'])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.message').isEqualTo('Popraw błędy w formularzu.')
                .jsonPath('$.errors.length()').isEqualTo(2)
                .jsonPath("\$.errors[?(@.field == 'name')].message").isEqualTo('To pole jest wymagane.')
                .jsonPath("\$.errors[?(@.field == 'email')].message").isEqualTo('Podaj poprawny adres e-mail.')
    }

    def "reports a field with a value of the wrong type"() {
        expect:
        client.post().uri('/api/1/errors/contact')
                .contentType(MediaType.APPLICATION_JSON)
                .body('{"name": "Ada", "age": "old"}')
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.errors[0].field').isEqualTo('age')
                .jsonPath('$.errors[0].message').isEqualTo('Wartość ma nieprawidłowy format.')
    }

    def "reports an invalid request parameter"() {
        expect:
        client.get().uri('/api/1/errors/param?page=0').exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.errors[0].field').isEqualTo('page')
                .jsonPath('$.errors[0].message').isEqualTo('Wartość musi być większa lub równa 1.')
    }
}
