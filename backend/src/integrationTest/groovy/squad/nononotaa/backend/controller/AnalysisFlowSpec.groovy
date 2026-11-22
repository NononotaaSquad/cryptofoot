package squad.nononotaa.backend.controller

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.util.concurrent.PollingConditions
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.testsupport.StubChainServer

/** The wired Analysis flow over HTTP on the synthetic demo-v1 fixture; the on-chain check talks to a local stub. */
class AnalysisFlowSpec extends BaseSpec {

    static final StubChainServer CHAIN = new StubChainServer()

    static final String BTC = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
    static final String BECH32 = 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'
    static final String ETH_EIP55 = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'
    static final String ETH = ETH_EIP55.toLowerCase()
    static final String CONTRACT = '0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae'

    @DynamicPropertySource
    static void chainProperties(DynamicPropertyRegistry registry) {
        registry.add('app.exclusion.onchain-check-enabled') { 'true' }
        registry.add('app.exclusion.eth-rpc-url') { CHAIN.url('/rpc') }
        registry.add('app.exclusion.blockchair-url') { CHAIN.url('') }
        registry.add('app.exclusion.timeout') { 'PT1S' }
    }

    @Autowired
    AnalysisRecorder recorder

    def polling = new PollingConditions(timeout: 15, delay: 0.1)

    def setup() {
        CHAIN.reset()
        CHAIN.rpcCode(ETH, '0x')
        CHAIN.rpcCode(CONTRACT, '0x6080604052')
    }

    def cleanupSpec() {
        CHAIN.stop()
    }

    private Map json(String uri) {
        new JsonSlurper().parseText(client.get().uri(uri).exchange().expectStatus().isOk().expectBody(String).returnResult().responseBody) as Map
    }

    private List jsonList(String uri) {
        new JsonSlurper().parseText(client.get().uri(uri).exchange().expectStatus().isOk().expectBody(String).returnResult().responseBody) as List
    }

    private Map start(String caseId) {
        new JsonSlurper().parseText(client.post().uri("/api/1/cases/${caseId}/analyses").exchange()
                .expectStatus().isAccepted().expectBody(String).returnResult().responseBody) as Map
    }

    private Map finished(String analysisId) {
        Map analysis = null
        polling.eventually {
            analysis = json("/api/1/analyses/${analysisId}")
            assert analysis.status != 'RUNNING'
        }
        analysis
    }

    def "one click starts a manual Analysis that runs every stage and is saved with its coverage"() {
        given:
        def caseId = createCase([nickname: 'zenek_fx'])

        when:
        def started = start(caseId)

        then:
        started.caseId == caseId
        started.trigger == 'MANUAL'
        started.status in ['RUNNING', 'PARTIAL']
        started.identifiers.nickname == 'zenek_fx'

        when:
        def analysis = finished(started.id as String)

        then:
        analysis.status == 'PARTIAL'
        analysis.finishedAt != null
        analysis.networks == ['BTC', 'ETH']
        analysis.algorithmVersion == 'confidence-2'
        analysis.attributionCount == 3
        analysis.excludedAddressCount == 1
        analysis.sources.collectEntries {
            [(it.sourceName): [
                    it.status,
                    it.synthetic,
                    it.mentionCount
                ]]
        } == [
            'synthetic'       : ['CHECKED', true, 3],
            'synthetic-broken': ['UNAVAILABLE', true, 0],
        ]
    }

    def "the report of the demo Analysis scores, sorts and justifies the person's addresses and excludes the contract"() {
        given:
        def analysisId = finished(start(createCase([nickname: 'zenek_fx'])).id as String).id

        when:
        def report = json("/api/1/analyses/${analysisId}/report")

        then:
        report.attributions.collect {
            [
                it.network,
                it.address,
                it.confidenceLevel,
                it.classificationStatus
            ]
        } == [
            [
                'BTC',
                BTC,
                75,
                'NOT_EXCLUDED'
            ],
            [
                'ETH',
                ETH,
                75,
                'NOT_EXCLUDED'
            ],
            [
                'BTC',
                BECH32,
                60,
                'NOT_EXCLUDED'
            ],
        ]
        report.attributions[0].mentions*.url as Set == [
            'https://forum.example/t/porady-p2p-od-zenka/123',
            'https://x.example/zenek_fx/status/555'
        ] as Set
        report.attributions[0].scoringRules*.rule == [
            'SALE_OR_SETTLEMENT',
            'INDEPENDENT_SOURCES',
            'NICKNAME_ONLY'
        ]
        report.attributions[0].justification == 'Adres podano w ogłoszeniu sprzedaży lub w kontekście rozliczenia (forum.example). ' +
                'Niezależne źródła różnego pochodzenia (forum.example, x.example) wskazują na ten sam adres. ' +
                'Adres łączy z badaną osobą tylko nick lub login, które mogą należeć do innej osoby.'
        report.attributions[2].scoringRules*.rule == [
            'SALE_OR_SETTLEMENT',
            'NICKNAME_ONLY'
        ]
        report.excludedAddresses.collect {
            [it.address, it.reason]
        } == [[CONTRACT, 'SMART_CONTRACT']]
        report.excludedAddresses[0].classificationSource == 'Ethereum JSON-RPC eth_getCode (127.0.0.1): executable code present'
        report.excludedAddresses[0].mentions*.url == ['https://p2p.example/ad/987']
        report.limitations == [
            'SOURCE_UNAVAILABLE',
            'ANALYSIS_INCOMPLETE'
        ]
        CHAIN.requests.toSet() == [
            "rpc ${ETH}".toString(),
            "rpc ${CONTRACT}".toString()
        ] as Set
    }

    def "with the person's profile address the post on that profile counts as the own profile"() {
        given:
        def caseId = createCase([nickname: 'zenek_fx', profileUrl: 'https://x.example/zenek_fx'])
        def analysisId = finished(start(caseId).id as String).id

        when:
        def report = json("/api/1/analyses/${analysisId}/report")

        then:
        report.attributions.collect {
            [
                it.address,
                it.confidenceLevel,
                it.scoringRules*.rule
            ]
        } == [
            [
                BTC,
                95,
                [
                    'OWN_PROFILE',
                    'INDEPENDENT_SOURCES'
                ]
            ],
            [
                ETH,
                95,
                [
                    'OWN_PROFILE',
                    'INDEPENDENT_SOURCES'
                ]
            ],
            [
                BECH32,
                60,
                [
                    'SALE_OR_SETTLEMENT',
                    'NICKNAME_ONLY'
                ]
            ],
        ]
        report.attributions[0].justification.startsWith('Adres został opublikowany na profilu badanej osoby (x.example).')
    }

    def "the Mentions of the demo Analysis carry provenance and validated addresses"() {
        given:
        def analysisId = finished(start(createCase([nickname: 'zenek_fx'])).id as String).id

        when:
        def mentions = jsonList("/api/1/analyses/${analysisId}/mentions")

        then:
        mentions.size() == 3
        mentions.every {
            it.sourceName == 'synthetic' && it.url && it.collectedAt && it.context.contains('zenek_fx')
        }
        mentions.collectMany {
            it.detectedAddresses
        }.every {
            it.valid && it.network in ['BTC', 'ETH'] && it.validationFailure == null
        }
        mentions.find {
            it.url == 'https://p2p.example/ad/987'
        }.detectedAddresses*.address == [BECH32, CONTRACT]
    }

    def "an identifier that no source mentions finishes with no results, distinct from a failure"() {
        given:
        def caseId = createCase([email: "nobody-${UUID.randomUUID()}@example.com".toString()])

        when:
        def analysis = finished(start(caseId).id as String)
        def report = json("/api/1/analyses/${analysis.id}/report")

        then:
        analysis.status == 'PARTIAL'
        analysis.attributionCount == 0
        analysis.sources.find {
            it.sourceName == 'synthetic'
        }.status == 'CHECKED'
        analysis.sources.find { it.sourceName == 'synthetic' }.mentionCount == 0
        report.attributions.isEmpty()
        report.limitations == [
            'SOURCE_UNAVAILABLE',
            'ANALYSIS_INCOMPLETE'
        ]
        jsonList("/api/1/analyses/${analysis.id}/mentions").isEmpty()
    }

    def "a wallet stored in canonical form finds the Mentions that spell it in EIP-55 and stays among the identifiers used"() {
        given:
        def caseId = createCase([walletAddress: ETH_EIP55])

        when:
        def analysis = finished(start(caseId).id as String)
        def mentions = jsonList("/api/1/analyses/${analysis.id}/mentions")

        then:
        analysis.identifiers.walletAddress == ETH
        mentions*.url as Set == [
            'https://forum.example/t/porady-p2p-od-zenka/123',
            'https://x.example/zenek_fx/status/555'
        ] as Set
    }

    def "the Analyst's wallet is input, not evidence: Mentions found only through it give no Confidence Level"() {
        given:
        def caseId = createCase([walletAddress: ETH_EIP55])

        when:
        def report = json("/api/1/analyses/${finished(start(caseId).id as String).id}/report")

        then:
        report.attributions.collect {
            [
                it.address,
                it.confidenceLevel,
                it.scoringRules*.rule
            ]
        } == [
            [
                BTC,
                0,
                ['NO_PERSON_LINK']
            ],
            [
                ETH,
                0,
                [
                    'ENTERED_BY_ANALYST',
                    'NO_PERSON_LINK'
                ]
            ],
        ]
        report.attributions[1].justification ==
                'Adres został wpisany przez analityka jako dane sprawy; samo wpisanie nie jest dowodem. ' +
                'Żadna wzmianka nie łączy tego adresu z danymi badanej osoby.'
    }

    def "a full name alone in the identifiers used finds nothing, even though the fixture names the person"() {
        given:
        def caseId = createCase([nickname: "nick-${UUID.randomUUID()}".toString(), fullName: 'Zenon Fikcyjny'])

        when:
        def analysis = finished(start(caseId).id as String)

        then:
        analysis.identifiers.fullName == 'Zenon Fikcyjny'
        jsonList("/api/1/analyses/${analysis.id}/mentions").isEmpty()
    }

    def "a rerun creates a new Analysis, keeps the earlier result, and both compare without changes"() {
        given:
        def caseId = createCase([nickname: 'zenek_fx'])
        def earlier = finished(start(caseId).id as String)
        def earlierReport = json("/api/1/analyses/${earlier.id}/report")

        when:
        def later = finished(start(caseId).id as String)
        def comparison = json("/api/1/cases/${caseId}/comparison?earlierAnalysisId=${earlier.id}&laterAnalysisId=${later.id}")

        then:
        later.id != earlier.id
        json("/api/1/analyses/${earlier.id}/report") == earlierReport
        jsonList("/api/1/cases/${caseId}/analyses")*.id == [later.id, earlier.id]
        comparison.changes.isEmpty()
        comparison.comparabilityLimitations == ['PARTIAL_ANALYSIS']
    }

    def "a second start while an Analysis of the Case is running is refused and starts nothing"() {
        given:
        def caseId = createCase([nickname: 'zenek_fx'])
        recorder.begin(new AnalysisRequest(caseId, AnalysisTrigger.MANUAL, [
            new CaseIdentifier(IdentifierType.NICKNAME, 'zenek_fx')
        ]))

        expect:
        client.post().uri("/api/1/cases/${caseId}/analyses").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath('$.message').isEqualTo('Analiza tej sprawy już trwa.')
        jsonList("/api/1/cases/${caseId}/analyses").size() == 1
    }

    def "an Analysis cannot be started for an unknown Case"() {
        expect:
        client.post().uri("/api/1/cases/missing-${UUID.randomUUID()}/analyses").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath('$.message').isEqualTo('Nie znaleziono sprawy o podanym identyfikatorze.')
    }

    def "Case details and the Case list show the latest Analysis"() {
        given:
        def older = createCase([nickname: "older-${UUID.randomUUID()}".toString()])
        def newer = createCase([nickname: 'zenek_fx'])
        def analysis = finished(start(newer).id as String)

        when:
        def details = json("/api/1/cases/${newer}")
        def list = jsonList('/api/1/cases')

        then:
        details.id == newer
        details.identifiers.nickname == 'zenek_fx'
        details.latestAnalysis.id == analysis.id
        details.latestAnalysis.status == 'PARTIAL'
        json("/api/1/cases/${older}").latestAnalysis == null
        list*.id.indexOf(newer) <list*.id.indexOf(older)
        list.find { it.id == newer }.latestAnalysis.id == analysis.id
    }

    def "an unknown Case is a 404"() {
        expect:
        client.get().uri("/api/1/cases/missing-${UUID.randomUUID()}").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath('$.message').isEqualTo('Nie znaleziono sprawy o podanym identyfikatorze.')
    }

    def "the latest Analysis of a Case is the newest one, whatever its status"() {
        given:
        def caseId = createCase([nickname: 'zenek_fx'])
        finished(start(caseId).id as String)
        def running = recorder.begin(new AnalysisRequest(caseId, AnalysisTrigger.SCHEDULED, []))

        expect:
        json("/api/1/cases/${caseId}").latestAnalysis.id == running
        json("/api/1/cases/${caseId}").latestAnalysis.status == 'RUNNING'
    }
}
