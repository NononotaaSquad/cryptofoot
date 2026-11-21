package squad.nononotaa.backend.util

import jakarta.validation.Validation
import spock.lang.Shared
import spock.lang.Specification
import squad.nononotaa.backend.dto.CreateCaseRequest

class NarrowingIdentifierSpec extends Specification {

    @Shared
    def validator = Validation.buildDefaultValidatorFactory().validator

    private violations(String nickname, String login, String email, String messengerId, String profileUrl, String fullName) {
        validator.validate(new CreateCaseRequest(nickname, login, email, messengerId, profileUrl, fullName))
    }

    def "a single narrowing identifier is enough: #field"() {
        expect:
        violations(*args).isEmpty()

        where:
        field         | args
        'nickname'    | [
            'nick',
            null,
            null,
            null,
            null,
            null
        ]
        'login'       | [
            null,
            'login',
            null,
            null,
            null,
            null
        ]
        'email'       | [
            null,
            null,
            'a@b.pl',
            null,
            null,
            null
        ]
        'messengerId' | [
            null,
            null,
            null,
            '@tg_user',
            null,
            null
        ]
        'profileUrl'  | [
            null,
            null,
            null,
            null,
            'https://example.test/u/1',
            null
        ]
    }

    def "a full name alone, or empty or blank narrowing identifiers, are rejected with a message naming the required kinds: #description"() {
        when:
        def result = violations(*args)

        then:
        result*.message == [
            'Podaj przynajmniej jeden identyfikator zaw\u0119\u017caj\u0105cy: pseudonim, login, adres e-mail, identyfikator w komunikatorze lub adres profilu. Samo imi\u0119 i nazwisko nie wystarcza.'
        ]

        where:
        description                       | args
        'nothing'                         | [
            null,
            null,
            null,
            null,
            null,
            null
        ]
        'only a full name'                | [
            null,
            null,
            null,
            null,
            null,
            'Jan Kowalski'
        ]
        'empty strings'                   | ['', '', '', '', '', null]
        'only white space'                | [
            '  ',
            '\t',
            null,
            '\n',
            ' ',
            'Jan Kowalski'
        ]
    }
}
