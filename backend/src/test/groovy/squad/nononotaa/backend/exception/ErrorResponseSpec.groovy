package squad.nononotaa.backend.exception

import org.springframework.http.HttpStatus
import spock.lang.Specification

class ErrorResponseSpec extends Specification {

    def "builds response from status and message"() {
        when:
        def response = new ErrorResponse(HttpStatus.NOT_FOUND, 'missing item')

        then:
        response.code == 404
        response.status == HttpStatus.NOT_FOUND
        response.message == 'missing item'
        response.errors == []
    }

    def "carries field errors"() {
        when:
        def response = new ErrorResponse(HttpStatus.BAD_REQUEST, 'Popraw błędy w formularzu.', [
            new FieldErrorResponse('name', 'To pole jest wymagane.')
        ])

        then:
        response.code == 400
        response.errors*.field == ['name']
    }

    def "uses exception message, falling back to its string form"() {
        expect:
        new ErrorResponse(HttpStatus.CONFLICT, exception).message == expected

        where:
        exception                          | expected
        new ConflictException('duplicate') | 'duplicate'
        new IllegalStateException()        | 'java.lang.IllegalStateException'
    }
}
