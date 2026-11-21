package squad.nononotaa.backend.analysis.web2

import static squad.nononotaa.testsupport.Web2Fixtures.BECH32
import static squad.nononotaa.testsupport.Web2Fixtures.BTC
import static squad.nononotaa.testsupport.Web2Fixtures.ETH

import groovy.json.JsonSlurper
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.util.concurrent.PollingConditions
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.testsupport.StubChainServer
import squad.nononotaa.testsupport.StubWeb2Server
import squad.nononotaa.testsupport.Web2Fixtures

/** The real Web2 sources against a local stub, through the real pipeline (detection, validation, scoring) and the REST API. */
class Web2PipelineSpec extends BaseSpec {

    static final StubWeb2Server SERVER = new StubWeb2Server()
    static final StubChainServer CHAIN = new StubChainServer()
    static final String BCT_PROFILE_URL = 'https://bitcointalk.org/index.php?action=profile;u=1234'

    @DynamicPropertySource
    static void web2Properties(DynamicPropertyRegistry registry) {
        registry.add('sources.synthetic.enabled') { 'false' }
        registry.add('app.exclusion.onchain-check-enabled') { 'true' }
        registry.add('app.exclusion.eth-rpc-url') { CHAIN.url('/rpc') }
        registry.add('app.exclusion.blockchair-url') { CHAIN.url('') }
        registry.add('app.exclusion.timeout') { 'PT1S' }
        // without synthetic data Address Activity needs the explorers, or every Analysis would be PARTIAL (D23-005)
        registry.add('app.address-activity.live-enabled') { 'true' }
        registry.add('app.address-activity.mempool-url') { CHAIN.url('') }
        registry.add('app.address-activity.blockscout-url') { CHAIN.url('') }
        registry.add('app.address-activity.timeout') { 'PT1S' }
        registry.add('sources.timeout') { '5s' }
        registry.add('sources.github.enabled') { 'true' }
        registry.add('sources.github.api-url') { SERVER.url() }
        registry.add('sources.github.timeout') { 'PT0.5S' }
        registry.add('sources.bitcointalk.enabled') { 'true' }
        registry.add('sources.bitcointalk.base-url') { SERVER.url() }
        registry.add('sources.bitcointalk.timeout') { 'PT0.5S' }
        registry.add('sources.bitcointalk.request-delay') { 'PT0S' }
    }

    def polling = new PollingConditions(timeout: 15, delay: 0.1)

    def setup() {
        SERVER.reset()
        CHAIN.reset()
        CHAIN.rpcCode(ETH.toLowerCase(), '0x')
        CHAIN.mempoolQuiet(BTC)
        CHAIN.mempoolQuiet(BECH32)
        CHAIN.blockscoutQuiet(ETH.toLowerCase())
    }

    def cleanupSpec() {
        SERVER.stop()
        CHAIN.stop()
    }

    private Map json(String uri) {
        new JsonSlurper().parseText(client.get().uri(uri).exchange().expectStatus().isOk().expectBody(String).returnResult().responseBody) as Map
    }

    private List jsonList(String uri) {
        new JsonSlurper().parseText(client.get().uri(uri).exchange().expectStatus().isOk().expectBody(String).returnResult().responseBody) as List
    }

    private Map analyse(Map identifiers) {
        def caseId = createCase(identifiers)
        def started = new JsonSlurper().parseText(client.post().uri("/api/1/cases/${caseId}/analyses").exchange()
                .expectStatus().isAccepted().expectBody(String).returnResult().responseBody) as Map
        Map analysis = null
        polling.eventually {
            analysis = json("/api/1/analyses/${started.id}")
            assert analysis.status != 'RUNNING'
        }
        analysis
    }

    private static Map coverage(Map analysis) {
        analysis.sources.collectEntries {
            [(it.sourceName): [
                    it.status,
                    it.synthetic,
                    it.mentionCount,
                    it.checkedAt != null
                ]]
        }
    }

    private void githubPerson() {
        SERVER.answer('/users/octo-dev', Web2Fixtures.githubUser('Octo-Dev', "Builder. BTC ${BTC} ETH ${ETH}"))
        SERVER.answer('/repos/Octo-Dev/Octo-Dev/readme', Web2Fixtures.longReadme())
    }

    def "GitHub profile and README reach detection, validation and scoring; the other sources report zero and not checked separately"() {
        given:
        githubPerson()

        when:
        def analysis = analyse([login: 'octo-dev'])
        def mentions = jsonList("/api/1/analyses/${analysis.id}/mentions")
        def report = json("/api/1/analyses/${analysis.id}/report")

        then:
        analysis.status == 'COMPLETED'
        coverage(analysis) == [
            github     : ['CHECKED', false, 2, true],
            bitcointalk: [
                'NOT_CHECKED',
                false,
                0,
                false
            ],
        ]
        report.limitations == []
        def byAddress = report.attributions.collectEntries {
            [(it.address): it]
        }
        byAddress.keySet() == [BTC, ETH.toLowerCase()] as Set
        report.attributions.every {
            it.addressActivityStatus == 'AVAILABLE' && !it.addressActivity.synthetic && it.addressActivity.transactionCount == 0
        }
        byAddress[ETH.toLowerCase()].mentions*.url as Set == [
            'https://github.com/Octo-Dev',
            'https://github.com/Octo-Dev/Octo-Dev'
        ] as Set
        byAddress[ETH.toLowerCase()].mentions.every {
            it.author == 'Octo-Dev' && it.context.length() <= 200 && it.context.contains(ETH)
        }
        byAddress[ETH.toLowerCase()].scoringRules*.rule == [
            'DISCUSSION',
            'SAME_SERVICE',
            'NICKNAME_ONLY'
        ]
        byAddress[ETH.toLowerCase()].confidenceLevel == 40
        byAddress[BTC].scoringRules*.rule == ['DISCUSSION', 'NICKNAME_ONLY']
        byAddress[BTC].mentions*.url == ['https://github.com/Octo-Dev']
        SERVER.requestLines().findAll {
            it.startsWith('GET /users/octo-dev') || it.contains('readme')
        }.size() == 2
    }

    def "a Bitcointalk profile URL gives the address field and signature as the person's own profile"() {
        given:
        SERVER.answer('/index.php?action=profile;u=1234', Web2Fixtures.bitcointalkProfile())

        when:
        def analysis = analyse([profileUrl: BCT_PROFILE_URL])
        def report = json("/api/1/analyses/${analysis.id}/report")

        then:
        analysis.status == 'COMPLETED'
        coverage(analysis) == [
            github     : [
                'NOT_CHECKED',
                false,
                0,
                false
            ],
            bitcointalk: ['CHECKED', false, 1, true],
        ]
        report.attributions*.address as Set == [BTC, BECH32] as Set
        report.attributions.every {
            it.mentions*.url == [BCT_PROFILE_URL] && it.mentions[0].author == 'Satoshi_Fan'
        }
        report.attributions.every {
            it.scoringRules*.rule == ['OWN_PROFILE'] && it.confidenceLevel == 80
        }
    }

    def "a Bitcointalk thread never turns another author's post or quote into the publication of the person"() {
        given:
        SERVER.answer('/index.php?topic=555.0', Web2Fixtures.bitcointalkThread([
            Web2Fixtures.bitcointalkPost('101', 'alice', "Send to ${BTC} thanks"),
            Web2Fixtures.bitcointalkPost('102', 'bob',
            "<div class=\"quoteheader\">Quote from: alice</div><div class=\"quote\">alice wrote ${ETH}</div>My address is ${BECH32}"),
        ]))

        when:
        def analysis = analyse([nickname: 'bob', profileUrl: 'https://bitcointalk.org/index.php?topic=555.0'])
        def mentions = jsonList("/api/1/analyses/${analysis.id}/mentions")
        def bitcointalk = mentions.findAll { it.sourceName == 'bitcointalk' }

        then:
        coverage(analysis).bitcointalk == ['CHECKED', false, 1, true]
        bitcointalk*.author == ['bob']
        bitcointalk*.url == [
            'https://bitcointalk.org/index.php?topic=555.msg102#msg102'
        ]
        bitcointalk[0].detectedAddresses*.address == [BECH32]
        !mentions*.detectedAddresses.flatten()*.address.contains(BTC)
        !mentions*.detectedAddresses.flatten()*.address.contains(ETH)
    }

    def "one unavailable source does not remove the results of the others and is never reported as zero"() {
        given:
        SERVER.answer('/users/multiuser', '{"message":"API rate limit exceeded"}', 403, ['X-RateLimit-Remaining': '0'])
        SERVER.answer('/index.php?action=profile;u=1234', Web2Fixtures.bitcointalkProfile())

        when:
        def analysis = analyse([login: 'multiuser', profileUrl: BCT_PROFILE_URL])
        def report = json("/api/1/analyses/${analysis.id}/report")

        then:
        analysis.status == 'PARTIAL'
        coverage(analysis) == [
            github     : [
                'UNAVAILABLE',
                false,
                0,
                false
            ],
            bitcointalk: ['CHECKED', false, 1, true],
        ]
        report.attributions*.address as Set == [BTC, BECH32] as Set
        report.limitations == [
            'SOURCE_UNAVAILABLE',
            'ANALYSIS_INCOMPLETE'
        ]
    }

    def "a repeated run reuses the stored mentions instead of duplicating them"() {
        given:
        SERVER.answer('/index.php?action=profile;u=1234', Web2Fixtures.bitcointalkProfile())

        when:
        def first = jsonList("/api/1/analyses/${analyse([profileUrl: BCT_PROFILE_URL]).id}/mentions")
        def second = jsonList("/api/1/analyses/${analyse([profileUrl: BCT_PROFILE_URL]).id}/mentions")

        then:
        first.size() == 1
        second*.id == first*.id
    }

    def "an Analysis in which no source could be checked fails and every source reports not checked"() {
        when:
        def analysis = analyse([email: 'ktos@example.org'])

        then:
        analysis.status == 'FAILED'
        coverage(analysis) == [
            github     : [
                'NOT_CHECKED',
                false,
                0,
                false
            ],
            bitcointalk: [
                'NOT_CHECKED',
                false,
                0,
                false
            ],
        ]
        SERVER.requests.isEmpty()
    }
}
