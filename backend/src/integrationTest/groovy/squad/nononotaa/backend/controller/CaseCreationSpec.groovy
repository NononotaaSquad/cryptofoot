package squad.nononotaa.backend.controller

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.repository.CaseRepository
import squad.nononotaa.backend.service.CaseService

class CaseCreationSpec extends BaseSpec {

    static final String REQUIRED = 'Podaj przynajmniej jeden identyfikator zaw\u0119\u017caj\u0105cy: pseudonim, login, adres e-mail, identyfikator w komunikatorze lub adres profilu. Samo imi\u0119 i nazwisko nie wystarcza.'

    @Autowired
    CaseRepository repository

    @Autowired
    CaseService caseService

    private post(Map body) {
        client.post().uri('/api/1/cases').contentType(MediaType.APPLICATION_JSON).body(body).exchange()
    }

    private Map created(Map body) {
        new JsonSlurper().parseText(post(body).expectStatus().isCreated().expectBody(String).returnResult().responseBody) as Map
    }

    private Map rejected(Map body) {
        new JsonSlurper().parseText(post(body).expectStatus().isBadRequest().expectBody(String).returnResult().responseBody) as Map
    }

    def "a Case is created from a single #field with a stable id and saved in MongoDB"() {
        when:
        def response = created([(field): value])
        def saved = repository.findById(response.id as String).get()

        then:
        response.id
        response.createdAt
        response.latestAnalysis == null
        response.identifiers[field] == value
        response.identifiers.findAll {
            it.value != null
        }.keySet() == [field] as Set
        saved.createdAt != null
        saved.identifiers."$field" == value

        where:
        field         | value
        'nickname'    | 'nick'
        'login'       | 'login'
        'email'       | 'ala@example.pl'
        'messengerId' | '@tg_user'
        'profileUrl'  | 'https://example.test/u/1'
    }

    def "a Case is created from the full set of data"() {
        given:
        def body = [nickname: 'nick', login: 'login', email: 'ala@example.pl', messengerId: '@tg', profileUrl: 'https://example.test/u/1', fullName: 'Jan Kowalski']

        when:
        def response = created(body)

        then:
        response.identifiers == body
        repository.findById(response.id as String).get().identifiers.fullName == 'Jan Kowalski'
    }

    def "a full name may accompany a narrowing identifier"() {
        expect:
        created([nickname: 'nick', fullName: 'Jan Kowalski']).identifiers.fullName == 'Jan Kowalski'
    }

    def "empty input, a full name alone and white space only are rejected with the Polish message naming the required kinds: #description"() {
        when:
        def error = rejected(body)

        then:
        error.code == 400
        error.message == 'Popraw b\u0142\u0119dy w formularzu.'
        error.errors.size() == 1
        error.errors[0].field == null
        error.errors[0].message == REQUIRED

        where:
        description                            | body
        'empty object'                         | [:]
        'only a full name'                     | [fullName: 'Jan Kowalski']
        'blank strings'                        | [nickname: '', login: '', email: '', messengerId: '', profileUrl: '']
        'white space only with a full name'    | [nickname: '  ', login: '\t', email: '   ', messengerId: '\n', profileUrl: ' ', fullName: 'Jan Kowalski']
    }

    def "nothing is stored for a rejected request"() {
        given:
        def before = repository.count()

        when:
        post([fullName: 'Jan Kowalski']).expectStatus().isBadRequest()

        then:
        repository.count() == before
    }

    def "values are trimmed, blank optional fields are not stored"() {
        when:
        def response = created([nickname: '  nick  ', login: '   ', fullName: '  Jan Kowalski '])

        then:
        response.identifiers.nickname == 'nick'
        response.identifiers.login == null
        response.identifiers.fullName == 'Jan Kowalski'
        repository.findById(response.id as String).get().identifiers.login == null
    }

    def "an invalid e-mail is rejected on the e-mail field"() {
        when:
        def error = rejected([nickname: 'nick', email: 'not-an-email'])

        then:
        error.errors*.field == ['email']
        error.errors[0].message == 'Podaj poprawny adres e-mail.'
    }

    def "there is no deduplication: the same data creates two independent Cases"() {
        when:
        def first = created([nickname: 'same-nick'])
        def second = created([nickname: 'same-nick'])

        then:
        first.id != second.id
        repository.findById(first.id as String).isPresent()
        repository.findById(second.id as String).isPresent()
    }

    def "a cryptocurrency address has no input field: it is ignored next to an identifier and is no identifier alone"() {
        when:
        def accepted = created([nickname: 'nick', address: '0x52908400098527886E0F7030069857D2E4169EE7'])

        then:
        accepted.identifiers.nickname == 'nick'
        !accepted.containsKey('address')
        !accepted.identifiers.containsKey('address')

        when:
        def error = rejected([address: '0x52908400098527886E0F7030069857D2E4169EE7', cryptoAddress: 'bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq'])

        then:
        error.errors[0].message == REQUIRED
    }

    def "values in the identifier fields are not scanned for addresses"() {
        expect:
        created([nickname: '0x52908400098527886E0F7030069857D2E4169EE7']).identifiers.nickname == '0x52908400098527886E0F7030069857D2E4169EE7'
    }

    def "Analyses can refer to the Case id: its current identifiers are available to the Analysis flow"() {
        given:
        def response = created([nickname: 'nick', email: 'ala@example.pl', fullName: 'Jan Kowalski'])

        expect:
        caseService.currentIdentifiers(response.id as String) == [
            new CaseIdentifier(IdentifierType.NICKNAME, 'nick'),
            new CaseIdentifier(IdentifierType.EMAIL, 'ala@example.pl'),
            new CaseIdentifier(IdentifierType.FULL_NAME, 'Jan Kowalski'),
        ]
        caseService.currentIdentifiers('does-not-exist') == null
    }
}