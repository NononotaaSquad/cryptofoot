package squad.nononotaa.backend.controller

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.ExclusionClassifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.repository.CaseRepository
import squad.nononotaa.backend.repository.MentionRepository
import squad.nononotaa.backend.service.CaseService

class CaseCreationSpec extends BaseSpec {

    static final String REQUIRED = 'Podaj przynajmniej jeden identyfikator zaw\u0119\u017caj\u0105cy: pseudonim, login, adres e-mail, identyfikator w komunikatorze, adres profilu lub adres portfela kryptowalutowego. Samo imi\u0119 i nazwisko nie wystarcza.'
    static final String BTC = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
    static final String BECH32 = 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'
    static final String ETH = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'

    @Autowired
    CaseRepository repository

    @Autowired
    MentionRepository mentions

    @Autowired
    AnalysisRecorder recorder

    @Autowired
    CaseService caseService

    @Autowired
    ExclusionClassifier classifier

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

    def "a Case is created from a wallet address alone and stores only the canonical form: #description"() {
        when:
        def response = created([walletAddress: input])
        def saved = repository.findById(response.id as String).get()

        then:
        response.identifiers.walletAddress == canonical
        response.identifiers.findAll {
            it.value != null
        }.keySet() == ['walletAddress'] as Set
        saved.identifiers.walletAddress == canonical
        !saved.toString().contains(input) || input == canonical

        where:
        description                | input                  | canonical
        'BTC Base58 kept as is'    | BTC                    | BTC
        'ETH lowercased'           | ETH                    | ETH.toLowerCase()
        'bech32 lowercased'        | BECH32.toUpperCase()   | BECH32
        'white space trimmed'      | "  ${BTC}  ".toString() | BTC
    }

    def "a wallet address works together with other identifiers"() {
        when:
        def response = created([nickname: 'nick', walletAddress: ETH, fullName: 'Jan Kowalski'])

        then:
        response.identifiers.nickname == 'nick'
        response.identifiers.walletAddress == ETH.toLowerCase()
        response.identifiers.fullName == 'Jan Kowalski'
    }

    def "a Case is created from the full set of data"() {
        given:
        def body = [nickname: 'nick', login: 'login', email: 'ala@example.pl', messengerId: '@tg', profileUrl: 'https://example.test/u/1', fullName: 'Jan Kowalski', walletAddress: BTC]

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
        description                         | body
        'empty object'                      | [:]
        'only a full name'                  | [fullName: 'Jan Kowalski']
        'blank strings'                     | [nickname: '', login: '', email: '', messengerId: '', profileUrl: '', walletAddress: '']
        'white space only with a full name' | [nickname: '  ', login: '\t', email: '   ', messengerId: '\n', profileUrl: ' ', walletAddress: '   ', fullName: 'Jan Kowalski']
    }

    def "an invalid or unsupported wallet address is rejected on its field and nothing is stored: #description"() {
        given:
        def before = repository.count()

        when:
        def error = rejected(body)

        then:
        error.errors.size() == 1
        error.errors[0].field == 'walletAddress'
        error.errors[0].message == message
        repository.count() == before

        where:
        description                       | body                                                                   | message
        'malformed ETH alone'             | [walletAddress: '0x123']                                               | 'Adres portfela ma nieprawid\u0142owy format.'
        'ETH checksum mismatch alone'     | [walletAddress: '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAEd']         | 'Adres portfela ma nieprawid\u0142ow\u0105 sum\u0119 kontroln\u0105.'
        'unsupported network alone'       | [walletAddress: 'LdP8Qox1VAhCzLJNqrr74YovaWYyNBUWvL']                  | 'Nieobs\u0142ugiwany format adresu portfela. Obs\u0142ugiwane s\u0105 adresy Bitcoin (BTC) i Ethereum (ETH).'
        'invalid wallet with a nickname'  | [nickname: 'nick', walletAddress: 'hello world']                    | 'Nieobs\u0142ugiwany format adresu portfela. Obs\u0142ugiwane s\u0105 adresy Bitcoin (BTC) i Ethereum (ETH).'
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
        def first = created([nickname: 'same-nick', walletAddress: BTC])
        def second = created([nickname: 'same-nick', walletAddress: BTC])

        then:
        first.id != second.id
        repository.findById(first.id as String).isPresent()
        repository.findById(second.id as String).isPresent()
    }

    def "an unknown address property is ignored and other fields are not scanned for addresses"() {
        when:
        def accepted = created([nickname: '0x52908400098527886E0F7030069857D2E4169EE7', address: BTC])

        then:
        accepted.identifiers.nickname == '0x52908400098527886E0F7030069857D2E4169EE7'
        accepted.identifiers.walletAddress == null
        !accepted.identifiers.containsKey('address')

        when:
        def error = rejected([address: BTC, cryptoAddress: BECH32])

        then:
        error.errors[0].message == REQUIRED
    }

    def "the wallet entered by the Analyst is an identifier of the Case, not a Mention or evidence"() {
        given:
        def mentionsBefore = mentions.count()

        when:
        def response = created([walletAddress: ETH])

        then:
        mentions.count() == mentionsBefore
        response.latestAnalysis == null
        !response.containsKey('mentions')
    }

    def "an exchange address entered as the wallet is accepted as input but stays an Excluded Address in the classification flow"() {
        given:
        def krakenHotWallet = '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0'

        when:
        def response = created([walletAddress: krakenHotWallet])
        def canonical = response.identifiers.walletAddress as String
        def result = classifier.classify([
            new Candidate('ETH', canonical, ['m1'])
        ])

        then:
        canonical == krakenHotWallet
        result.accepted.isEmpty()
        result.unclassified.isEmpty()
        result.excluded*.reason == [ExclusionReason.EXCHANGE]
    }

    def "Analyses can refer to the Case id: the identifiers handed to the Analysis flow include the canonical wallet"() {
        given:
        def response = created([nickname: 'nick', walletAddress: ETH, fullName: 'Jan Kowalski'])

        expect:
        caseService.currentIdentifiers(response.id as String) == [
            new CaseIdentifier(IdentifierType.NICKNAME, 'nick'),
            new CaseIdentifier(IdentifierType.WALLET_ADDRESS, ETH.toLowerCase()),
            new CaseIdentifier(IdentifierType.FULL_NAME, 'Jan Kowalski'),
        ]
        caseService.currentIdentifiers('does-not-exist') == null
    }

    def "a saved Analysis keeps the wallet among the identifiers used"() {
        given:
        def response = created([walletAddress: BTC])
        def identifiers = caseService.currentIdentifiers(response.id as String)

        when:
        def analysisId = recorder.begin(new AnalysisRequest(response.id as String, AnalysisTrigger.MANUAL, identifiers))

        then:
        client.get().uri("/api/1/analyses/${analysisId}").exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath('$.identifiers.walletAddress').isEqualTo(BTC)
                .jsonPath('$.identifiers.nickname').isEmpty()
    }
}