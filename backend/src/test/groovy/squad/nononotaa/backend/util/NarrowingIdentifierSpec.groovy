package squad.nononotaa.backend.util

import jakarta.validation.Validation
import spock.lang.Shared
import spock.lang.Specification
import squad.nononotaa.backend.dto.CreateCaseRequest

class NarrowingIdentifierSpec extends Specification {

    static final String REQUIRED = 'Podaj przynajmniej jeden identyfikator zaw\u0119\u017caj\u0105cy: pseudonim, login, adres e-mail, identyfikator w komunikatorze, adres profilu lub adres portfela kryptowalutowego. Samo imi\u0119 i nazwisko nie wystarcza.'
    static final String BTC = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
    static final String BECH32 = 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'
    static final String ETH = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'

    @Shared
    def validator = Validation.buildDefaultValidatorFactory().validator

    private violations(String nickname, String login, String email, String messengerId, String profileUrl, String fullName, String walletAddress = null) {
        validator.validate(new CreateCaseRequest(nickname, login, email, messengerId, profileUrl, fullName, walletAddress))
    }

    def "a single narrowing identifier is enough: #field"() {
        expect:
        violations(*args).isEmpty()

        where:
        field           | args
        'nickname'      | [
            'nick',
            null,
            null,
            null,
            null,
            null,
            null
        ]
        'login'         | [
            null,
            'login',
            null,
            null,
            null,
            null,
            null
        ]
        'email'         | [
            null,
            null,
            'a@b.pl',
            null,
            null,
            null,
            null
        ]
        'messengerId'   | [
            null,
            null,
            null,
            '@tg_user',
            null,
            null,
            null
        ]
        'profileUrl'    | [
            null,
            null,
            null,
            null,
            'https://example.test/u/1',
            null,
            null
        ]
        'walletAddress' | [
            null,
            null,
            null,
            null,
            null,
            null,
            BTC
        ]
    }

    def "a full name alone, or empty or blank narrowing identifiers, are rejected with a message naming the required kinds: #description"() {
        when:
        def result = violations(*args)

        then:
        result*.message == [REQUIRED]

        where:
        description        | args
        'nothing'          | [
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ]
        'only a full name' | [
            null,
            null,
            null,
            null,
            null,
            'Jan Kowalski',
            null
        ]
        'empty strings'    | ['', '', '', '', '', null, '']
        'only white space' | [
            '  ',
            '\t',
            null,
            '\n',
            ' ',
            'Jan Kowalski',
            '  '
        ]
    }

    def "a valid BTC or ETH wallet address is accepted: #address"() {
        expect:
        violations(null, null, null, null, null, null, address).isEmpty()

        where:
        address << [
            BTC,
            BECH32,
            BECH32.toUpperCase(),
            ETH,
            ETH.toLowerCase(),
            '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy'
        ]
    }

    def "an invalid wallet address is rejected on the wallet field with a message for the failure: #description"() {
        when:
        def result = violations('nick', null, null, null, null, null, address)

        then:
        result*.propertyPath*.toString() == ['walletAddress']
        result*.message == [message]

        where:
        description                    | address                                       | message
        'malformed ETH address'        | '0x123'                                       | 'Adres portfela ma nieprawid\u0142owy format.'
        'ETH checksum mismatch'        | '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAEd'  | 'Adres portfela ma nieprawid\u0142ow\u0105 sum\u0119 kontroln\u0105.'
        'unsupported (Litecoin)'       | 'LdP8Qox1VAhCzLJNqrr74YovaWYyNBUWvL'          | 'Nieobs\u0142ugiwany format adresu portfela. Obs\u0142ugiwane s\u0105 adresy Bitcoin (BTC) i Ethereum (ETH).'
        'free text'               | 'hello world'                              | 'Nieobs\u0142ugiwany format adresu portfela. Obs\u0142ugiwane s\u0105 adresy Bitcoin (BTC) i Ethereum (ETH).'
    }
}