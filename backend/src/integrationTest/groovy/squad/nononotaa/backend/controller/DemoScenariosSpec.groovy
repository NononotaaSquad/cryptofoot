package squad.nononotaa.backend.controller

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.util.concurrent.PollingConditions
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.synthetic.SyntheticMentionSource
import squad.nononotaa.testsupport.StubChainServer

// frozen expected results of the fictional demo scenarios S1-S11 of fixtures/demo-v1.json (demo-scenarios.md)
class DemoScenariosSpec extends BaseSpec {

    static final StubChainServer CHAIN = new StubChainServer()

    static final String ETH_HALINA = '0xe73a66581205c9585215196c89c5648dea1420c6'
    static final String BTC_CELINA = 'bc1qym2cd57yxhzgwyk9g9st5gwu0yzenphmgav703'
    static final String BTC_CELINA_SCAM = '1BitBDDpit1FNtTRjXS9BWosurnHvZP3BY'
    static final String BTC_DAREK = 'bc1q6qq06qyp98pvwckcmqgmnqgjvw2u8r4pp4hzpm'
    static final String BTC_DAREK_NAMESAKE = '18EvUarRFEFrmDBJg1J2brT6epaSqdaGGu'
    static final String BTC_EWA = 'bc1qfjy9mr8n5jtrwevzqrj38axjkstn9tlhupkg28'
    static final String BTC_FRANEK_BAD_CHECKSUM = '1GbntFg1DvKoWUfibVf9Fsw4myaYKz5gD4'
    static final String ETH_FRANEK_BAD_CASE = '0x5e3223B2D31A5689e6Ca1D82528042c9Ca2b7809'
    static final String BTC_GRAZKA = 'bc1qy68k8774jj4nkugjw0vkkzms28359lkp56uhrj'
    static final String BTC_HENIO_FAR = '1C6U3KTce8VaBY9X37DchkG4dKF2kaDRfb'
    static final String BTC_IRENA = 'bc1q592ze7z6w4pl5mp4xuaa494cjjt5fq7ksj7d8k'
    static final String BTC_OLIWER_SAVINGS = 'bc1qtcrn6cnv7ycrsgsafch7mdtmtx5ahqxxla2pca'
    static final String BTC_OLIWER_SALE = '14MqWBPG6bAYFyxBqgWwBcJVLiCeodYcoy'
    static final String BTC_OLIWER_NEW = 'bc1q3pu24gfage2sqesa8gm6c245j5pzfkgse6tt0n'
    static final String BINANCE_BTC = '1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s'
    static final String KRAKEN = '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0'
    static final String ROUTER = '0x7a250d5630b4cf539739df2c5dacb4c659f2488d'
    static final String TORNADO = '0x722122df12d4e14e13ac3b6895a86e84145b6967'
    static final String UKRAINE = '0x165cd37b4c644c2921454429e7f9358d18a45e14'

    static final Map<String, String> FIELDS = [
        NICKNAME     : 'nickname',
        LOGIN        : 'login',
        EMAIL        : 'email',
        MESSENGER_ID : 'messengerId',
        PROFILE_URL  : 'profileUrl',
        FULL_NAME    : 'fullName',
        WALLET_ADDRESS: 'walletAddress'
    ]

    @DynamicPropertySource
    static void chainProperties(DynamicPropertyRegistry registry) {
        registry.add('app.exclusion.onchain-check-enabled') { 'true' }
        registry.add('app.exclusion.eth-rpc-url') { CHAIN.url('/rpc') }
        registry.add('app.exclusion.blockchair-url') { CHAIN.url('') }
        registry.add('app.exclusion.timeout') { 'PT1S' }
    }

    @Autowired
    SyntheticMentionSource source

    def polling = new PollingConditions(timeout: 15, delay: 0.1)

    def setup() {
        CHAIN.reset()
        CHAIN.rpcCode(ETH_HALINA, '0x')
        CHAIN.rpcCode(ROUTER, '0x6080604052')
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

    // runs the scenario with exactly the identifiers listed in the fixture
    private Map analyse(String scenarioId) {
        run(scenarioCase(scenarioId))
    }

    private String scenarioCase(String scenarioId) {
        def scenario = source.fixture.scenarios.find { it.id == scenarioId }
        createCase(scenario.identifiers.collectEntries {
            [(FIELDS[it.type]): it.value]
        })
    }

    // one more Analysis of the Case, as "Rozpocznij nową analizę" starts it
    private Map run(String caseId) {
        def started = new JsonSlurper().parseText(client.post().uri("/api/1/cases/${caseId}/analyses").exchange()
                .expectStatus().isAccepted().expectBody(String).returnResult().responseBody) as Map
        Map analysis = null
        polling.eventually {
            analysis = json("/api/1/analyses/${started.id}")
            assert analysis.status != 'RUNNING'
        }
        [analysis: analysis, report: json("/api/1/analyses/${started.id}/report"), mentions: jsonList("/api/1/analyses/${started.id}/mentions")]
    }

    private static List levels(Map report) {
        report.attributions.collect {
            [
                it.address,
                it.confidenceLevel,
                it.scoringRules*.rule
            ]
        }
    }

    private static Map activity(Map report, String address) {
        report.attributions.find { it.address == address }
    }

    def "the fixture lists the ten scenarios this spec freezes"() {
        expect:
        source.fixture.scenarios*.id == (1..11).collect { "S$it".toString() }
        source.fixture.scenarios*.category.toSet() == [
            'HAPPY_PATH',
            'FALSE_POSITIVE',
            'FALSE_NEGATIVE',
            'EDGE_CASE',
            'CHANGE_OVER_TIME'
        ] as Set
    }

    def "S1 happy path: own profile plus an independent sale ad, and an ETH cash-out through a swap, a gateway, P2P and an exchange"() {
        when:
        def report = analyse('S1').report
        def halina = activity(report, ETH_HALINA)

        then:
        levels(report) == [
            [
                ETH_HALINA,
                95,
                [
                    'OWN_PROFILE',
                    'INDEPENDENT_SOURCES'
                ]
            ]
        ]
        report.excludedAddresses.isEmpty()
        halina.classificationStatus == 'NOT_EXCLUDED'
        halina.addressActivityStatus == 'AVAILABLE'
        halina.addressActivity.totalReceived == 12.5
        halina.addressActivity.totalSent == 11.5
        halina.addressActivity.transactionCount == 6
        halina.addressActivity.cashOutSignals.collect {
            [it.kind, it.amount, it.unit]
        } == [
            [
                'STABLECOIN_SWAP',
                30000,
                'USDC'
            ],
            [
                'PAYMENT_GATEWAY',
                12000,
                'USDC'
            ],
            ['P2P', 1.5, 'ETH'],
            ['EXCHANGE', 15000, 'USDC']
        ]
        // the swap has no labelled counterparty; the other three come from the Etherscan tags (D23-010)
        halina.addressActivity.cashOutSignals*.counterpartyLabel.collect {
            it?.replaceAll(/^.*: /, '')
        } == [
            null,
            'Bity.com [fiat-gateway]',
            'Remitano 2 [remitano]',
            'Kraken 4 [kraken]'
        ]
    }

    def "S2 false positive: addresses of an exchange, a contract, a mixer and a fundraiser are never attributed"() {
        when:
        def report = analyse('S2').report

        then:
        report.attributions.isEmpty()
        report.excludedAddresses.collect {
            [it.address, it.reason]
        } as Set == [
            [BINANCE_BTC, 'EXCHANGE'],
            [KRAKEN, 'EXCHANGE'],
            [ROUTER, 'SMART_CONTRACT'],
            [TORNADO, 'MIXER'],
            [UKRAINE, 'FUNDRAISER']
        ] as Set
    }

    def "S3 false positive: an impostor's address quoted in a warning gives 0%, the own profile address 80%"() {
        when:
        def report = analyse('S3').report

        then:
        levels(report) == [
            [
                BTC_CELINA,
                80,
                ['OWN_PROFILE']
            ],
            [
                BTC_CELINA_SCAM,
                0,
                ['WARNING_CONTEXT']
            ]
        ]
        activity(report, BTC_CELINA).addressActivity.totalReceived == 0.05
        activity(report, BTC_CELINA_SCAM).addressActivityStatus == 'NOT_CHECKED'
    }

    def "S4 false positive: a namesake with another e-mail gives 0%, the own sale ad 60% with a transfer to an exchange"() {
        when:
        def report = analyse('S4').report

        then:
        levels(report) == [
            [
                BTC_DAREK,
                60,
                ['SALE_OR_SETTLEMENT']
            ],
            [
                BTC_DAREK_NAMESAKE,
                0,
                ['CONFLICTING_EMAIL']
            ]
        ]
        activity(report, BTC_DAREK).addressActivity.cashOutSignals.collect {
            [
                it.kind,
                it.amount,
                it.counterpartyAddress
            ]
        } == [
            ['EXCHANGE', 0.8, BINANCE_BTC]
        ]
    }

    def "S5 edge case: a copy on another service adds no bonus, and an empty history is real zero activity"() {
        when:
        def report = analyse('S5').report
        def ewa = activity(report, BTC_EWA)

        then:
        levels(report) == [
            [
                BTC_EWA,
                60,
                [
                    'SALE_OR_SETTLEMENT',
                    'COPIED_TEXT',
                    'NICKNAME_ONLY'
                ]
            ]
        ]
        ewa.addressActivityStatus == 'AVAILABLE'
        [
            ewa.addressActivity.totalReceived,
            ewa.addressActivity.totalSent,
            ewa.addressActivity.transactionCount,
            ewa.addressActivity.firstActivityAt
        ] == [0, 0, 0, null]
    }

    def "S6 edge case: addresses with a wrong checksum are rejected, never attributed"() {
        when:
        def result = analyse('S6')

        then:
        result.report.attributions.isEmpty()
        result.mentions.collectMany {
            it.detectedAddresses
        }.collect {
            [
                it.address,
                it.valid,
                it.validationFailure
            ]
        } == [
            [
                BTC_FRANEK_BAD_CHECKSUM,
                false,
                'CHECKSUM_MISMATCH'
            ],
            [
                ETH_FRANEK_BAD_CASE,
                false,
                'CHECKSUM_MISMATCH'
            ]
        ]
    }

    def "S7 edge case: a giveaway comment gives 10%, and missing activity data is unavailable, never zero"() {
        when:
        def report = analyse('S7').report

        then:
        levels(report) == [
            [
                BTC_GRAZKA,
                10,
                [
                    'MASS_COMMENT',
                    'NICKNAME_ONLY'
                ]
            ]
        ]
        activity(report, BTC_GRAZKA).addressActivityStatus == 'UNAVAILABLE'
        activity(report, BTC_GRAZKA).addressActivity == null
        report.limitations.contains('BLOCKCHAIN_DATA_UNAVAILABLE')
    }

    def "S8: a space-separated address is missed, but an address outside the excerpt is detected with 0% confidence"() {
        when:
        def result = analyse('S8')
        def forum = result.mentions.find {
            it.url == 'https://forum.example/t/henio/88'
        }
        def blog = result.mentions.find {
            it.url == 'https://blog.example/henio/o-mnie'
        }

        then:
        result.mentions.size() == 2
        forum.detectedAddresses.isEmpty()
        !blog.context.contains(BTC_HENIO_FAR)
        blog.detectedAddresses*.address == [BTC_HENIO_FAR]
        blog.detectedAddresses*.valid == [true]
        levels(result.report) == [
            [
                BTC_HENIO_FAR,
                0,
                ['NO_PERSON_LINK']
            ]
        ]
    }

    def "S9 false negative: a cash-out to an unlisted exchange deposit address gives no signal"() {
        when:
        def report = analyse('S9').report
        def irena = activity(report, BTC_IRENA)

        then:
        levels(report) == [
            [
                BTC_IRENA,
                80,
                ['OWN_PROFILE']
            ]
        ]
        irena.addressActivity.totalReceived == 4
        irena.addressActivity.totalSent == 3.9
        irena.addressActivity.transactionCount == 2
        irena.addressActivity.cashOutSignals.isEmpty()
        irena.addressActivity.coverageLimitations == [
            'LABELLED_COUNTERPARTIES_ONLY'
        ]
    }

    def "S10 edge case: nobody mentions the person, which is not a failure"() {
        when:
        def result = analyse('S10')

        then:
        result.mentions.isEmpty()
        result.report.attributions.isEmpty()
        result.analysis.sources.find {
            it.sourceName == 'synthetic'
        }.status == 'CHECKED'
        result.analysis.status != 'FAILED'
    }

    def "S11 change over time: each rerun of the Case sees the other state of the web, and the comparison lists what changed"() {
        given:
        def caseId = scenarioCase('S11')

        when:
        def first = run(caseId)
        def second = run(caseId)
        def third = run(caseId)
        def changes = json("/api/1/cases/${caseId}/comparison?earlierAnalysisId=${first.analysis.id}&laterAnalysisId=${second.analysis.id}")
        def back = json("/api/1/cases/${caseId}/comparison?earlierAnalysisId=${second.analysis.id}&laterAnalysisId=${third.analysis.id}")

        then: 'odd Analyses: a forum post and a sale ad'
        levels(first.report) == [
            [
                BTC_OLIWER_SALE,
                60,
                [
                    'SALE_OR_SETTLEMENT',
                    'NICKNAME_ONLY'
                ]
            ],
            [
                BTC_OLIWER_SAVINGS,
                40,
                [
                    'DISCUSSION',
                    'NICKNAME_ONLY'
                ]
            ]
        ]
        levels(third.report) == levels(first.report)

        and: 'even Analyses: the ad is gone, the savings address and a new one are on the own profile'
        levels(second.report) == [
            [
                BTC_OLIWER_SAVINGS,
                95,
                [
                    'OWN_PROFILE',
                    'INDEPENDENT_SOURCES'
                ]
            ],
            [
                BTC_OLIWER_NEW,
                80,
                ['OWN_PROFILE']
            ]
        ]
        activity(second.report, BTC_OLIWER_NEW).addressActivity.cashOutSignals.collect {
            [
                it.kind,
                it.amount,
                it.counterpartyAddress
            ]
        } == [
            ['EXCHANGE', 0.35, BINANCE_BTC]
        ]
        [first, second, third].every {
            it.report.attributions.every { attribution ->
                attribution.addressActivityStatus == 'AVAILABLE'
            }
        }

        and: 'the comparison shows a new Attribution, a changed Confidence Level and one not observed (sorted by address)'
        changes.changes.collect {
            [
                it.address,
                it.change,
                it.earlierConfidenceLevel,
                it.laterConfidenceLevel
            ]
        } == [
            [
                BTC_OLIWER_SALE,
                'NOT_OBSERVED',
                60,
                null
            ],
            [
                BTC_OLIWER_NEW,
                'NEW',
                null,
                80
            ],
            [
                BTC_OLIWER_SAVINGS,
                'CONFIDENCE_LEVEL_CHANGED',
                40,
                95
            ]
        ]
        // locally synthetic-broken makes every Analysis partial, so the absence cannot be confirmed
        changes.changes.find {
            it.change == 'NOT_OBSERVED'
        }.absenceVerifiable == false
        changes.comparabilityLimitations == ['PARTIAL_ANALYSIS']

        and: 'the next rerun flips back, so a rehearsed demo Case still has changes to show'
        back.changes.collect {
            [it.address, it.change]
        } == [
            [BTC_OLIWER_SALE, 'NEW'],
            [
                BTC_OLIWER_NEW,
                'NOT_OBSERVED'
            ],
            [
                BTC_OLIWER_SAVINGS,
                'CONFIDENCE_LEVEL_CHANGED'
            ]
        ]
    }
}
