package squad.nononotaa.backend.service

import groovy.json.JsonSlurper
import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.util.concurrent.PollingConditions
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AddressActivityCollector
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.config.AddressActivityProperties
import squad.nononotaa.backend.model.AddressActivityCoverageLimitation
import squad.nononotaa.backend.model.AddressActivityStatus
import squad.nononotaa.backend.model.CashOutSignalKind
import squad.nononotaa.testsupport.StubChainServer

// the real AddressActivityCollector bean with live explorers on, against a local stub of mempool.space and Blockscout
class AddressActivityLiveSpec extends BaseSpec {

    static final StubChainServer CHAIN = new StubChainServer()

    static final String BTC = 'bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq'
    static final String BINANCE = '1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s'
    static final String ETH = '0xd1220a0cf47c7b9be7a2e6ba89f429762e7b9adb'
    static final String ETH_EIP55 = '0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb'
    static final String KRAKEN = '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0'
    static final String PAIR = '0xb4e16d0168e52d35cacd2c6185b44281ec28c9dc'
    static final String DEMO_ETH = '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
    static final String CONTRACT = '0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae'
    static final String SWAP_TX = '0x' + 'a1' * 32
    static final String EXIT_TX = '0x' + 'b2' * 32

    @DynamicPropertySource
    static void explorerProperties(DynamicPropertyRegistry registry) {
        registry.add('app.address-activity.live-enabled') { 'true' }
        registry.add('app.address-activity.mempool-url') { CHAIN.url('') }
        registry.add('app.address-activity.blockscout-url') { CHAIN.url('') }
        registry.add('app.address-activity.timeout') { 'PT1S' }
        // no ETH candidate may reach the internet: unlisted ETH addresses stay unclassified
        registry.add('app.exclusion.onchain-check-enabled') { 'false' }
    }

    @Autowired
    AddressActivityCollector collector

    @Autowired
    AddressActivityProperties properties

    def polling = new PollingConditions(timeout: 15, delay: 0.1)

    def setup() {
        CHAIN.reset()
    }

    def cleanupSpec() {
        CHAIN.stop()
    }

    private static ScoredAttribution attribution(String network, String address, int level = 60) {
        new ScoredAttribution(new Candidate(network, address, ['m1']), level, [])
    }

    private def collect(ScoredAttribution attribution) {
        collector.collect([attribution])[attribution.candidate]
    }

    private void mempool(int txCount) {
        CHAIN.explorer("/api/address/$BTC", """{"address":"$BTC","chain_stats":{"funded_txo_count":3,"funded_txo_sum":150000000,
            "spent_txo_count":1,"spent_txo_sum":100000000,"tx_count":$txCount},"mempool_stats":{"tx_count":1}}""")
        CHAIN.explorer("/api/address/$BTC/txs", """[
            {"txid":"u1","status":{"confirmed":false},"vin":[],"vout":[{"scriptpubkey_address":"$BTC","value":5}]},
            {"txid":"t3","status":{"confirmed":true,"block_time":1767225600},
             "vin":[{"prevout":{"scriptpubkey_address":"$BTC","value":100000000}}],
             "vout":[{"scriptpubkey_address":"$BINANCE","value":70000000},{"scriptpubkey_address":"$BTC","value":29990000},
                     {"scriptpubkey_type":"op_return","value":0}]},
            {"txid":"t2","status":{"confirmed":true,"block_time":1764547200},
             "vin":[{"prevout":{"scriptpubkey_address":"3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy","value":60000000}}],
             "vout":[{"scriptpubkey_address":"$BTC","value":50000000},{"scriptpubkey_address":"3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy","value":9990000}]},
            {"txid":"t1","status":{"confirmed":true,"block_time":1761955200},
             "vin":[{"is_coinbase":true}],
             "vout":[{"scriptpubkey_address":"$BTC","value":100000000}]}
        ]""")
    }

    private void blockscout() {
        def base = "/api/v2/addresses/$ETH"
        CHAIN.explorer("$base/counters", '{"transactions_count":"3","token_transfers_count":"3","gas_usage_count":"42000","validations_count":"0"}')
        CHAIN.explorer("$base/transactions", """{"items":[
            {"hash":"$SWAP_TX","timestamp":"2026-02-01T10:00:00.000000Z","status":"ok","value":"1500000000000000000",
             "from":{"hash":"$ETH_EIP55"},"to":{"hash":"0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D"}},
            {"hash":"0x${'c3' * 32}","timestamp":"2026-01-20T10:00:00.000000Z","status":"error","value":"9000000000000000000",
             "from":{"hash":"$ETH_EIP55"},"to":{"hash":"$KRAKEN"}},
            {"hash":"0x${'d4' * 32}","timestamp":"2026-01-10T10:00:00.000000Z","status":"ok","value":"2000000000000000000",
             "from":{"hash":"0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359"},"to":{"hash":"$ETH_EIP55"}}
        ],"next_page_params":null}""")
        CHAIN.explorer("$base/token-transfers?type=ERC-20", """{"items":[
            {"transaction_hash":"$EXIT_TX","timestamp":"2026-02-02T10:00:00.000000Z","from":{"hash":"$ETH_EIP55"},"to":{"hash":"$KRAKEN"},
             "token":{"address_hash":"0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48","symbol":"USDC","decimals":"6"},"total":{"value":"3500000000","decimals":"6"}},
            {"transaction_hash":"$SWAP_TX","timestamp":"2026-02-01T10:00:00.000000Z","from":{"hash":"$PAIR"},"to":{"hash":"$ETH_EIP55"},
             "token":{"address_hash":"0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48","symbol":"USDC","decimals":"6"},"total":{"value":"4000000000","decimals":"6"}},
            {"tx_hash":"0x${'e5' * 32}","timestamp":"2026-01-15T10:00:00.000000Z","from":{"hash":"0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359"},"to":{"hash":"$ETH_EIP55"},
             "token":{"address":"0x1111111111111111111111111111111111111111","symbol":"USDC","decimals":"6"},"total":{"value":"9000000000"}}
        ],"next_page_params":null}""")
    }

    private Map finishedReport(String analysisId) {
        Map report = null
        polling.eventually {
            def body = client.get().uri("/api/1/analyses/${analysisId}/report").exchange().returnResult(String).responseBody
            report = new JsonSlurper().parseText(body) as Map
            assert report.analysis?.status in [
                'PARTIAL',
                'COMPLETED',
                'FAILED'
            ]
        }
        report
    }

    def "binds the live explorer configuration"() {
        expect:
        properties.liveEnabled
        properties.mempoolUrl == URI.create(CHAIN.url(''))
        properties.blockscoutUrl == URI.create(CHAIN.url(''))
        properties.timeout == Duration.ofSeconds(1)
        properties.stablecoinSwapThreshold == 1000
    }

    def "mempool.space: exact totals, confirmed transactions only and a transfer to a listed exchange"() {
        given:
        mempool(3)

        when:
        def outcome = collect(attribution('BTC', BTC))

        then:
        outcome.status == AddressActivityStatus.AVAILABLE
        with(outcome.activity) {
            sourceName == 'mempool.space Bitcoin explorer API (127.0.0.1)'
            !synthetic
            checkedAt != null
            unit == 'BTC'
            totalReceived == 1.5
            totalSent == 1
            transactionCount == 3
            firstActivityAt.toString() == '2025-11-01T00:00:00Z'
            lastActivityAt.toString() == '2026-01-01T00:00:00Z'
            coverageLimitations == [
                AddressActivityCoverageLimitation.LABELLED_COUNTERPARTIES_ONLY
            ]
            cashOutSignals.size() == 1
        }
        with(outcome.activity.cashOutSignals[0]) {
            kind == CashOutSignalKind.EXCHANGE
            transactionId == 't3'
            transactionUrl == CHAIN.url('/tx/t3')
            amount == 0.7
            unit == 'BTC'
            counterpartyAddress == BINANCE
            counterpartyLabel == 'Own label list (https://www.walletexplorer.com/address/1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s): Binance.com'
        }
        CHAIN.requests == [
            "explorer /api/address/$BTC".toString(),
            "explorer /api/address/$BTC/txs".toString()
        ]
    }

    def "mempool.space: a long history keeps the stated totals but not the first activity, and says only recent transactions were read"() {
        given:
        mempool(40)

        when:
        def activity = collect(attribution('BTC', BTC)).activity

        then:
        activity.totalReceived == 1.5
        activity.transactionCount == 40
        activity.firstActivityAt == null
        activity.lastActivityAt.toString() == '2026-01-01T00:00:00Z'
        activity.coverageLimitations == [
            AddressActivityCoverageLimitation.RECENT_TRANSACTIONS_ONLY,
            AddressActivityCoverageLimitation.LABELLED_COUNTERPARTIES_ONLY
        ]
    }

    def "Blockscout: coin totals, a swap into USDC and a USDC transfer to an exchange; a failed transaction and a look-alike token count for nothing"() {
        given:
        blockscout()

        when:
        def outcome = collect(attribution('ETH', ETH))

        then:
        outcome.status == AddressActivityStatus.AVAILABLE
        with(outcome.activity) {
            sourceName == 'Blockscout Ethereum explorer API (127.0.0.1)'
            unit == 'ETH'
            totalReceived == 2
            totalSent == 1.5
            transactionCount == 3
            firstActivityAt.toString() == '2026-01-10T10:00:00Z'
            lastActivityAt.toString() == '2026-02-02T10:00:00Z'
            coverageLimitations == [
                AddressActivityCoverageLimitation.LABELLED_COUNTERPARTIES_ONLY,
                AddressActivityCoverageLimitation.INTERNAL_TRANSFERS_NOT_COUNTED
            ]
            cashOutSignals.collect {
                [
                    it.kind,
                    it.transactionId,
                    it.amount,
                    it.unit,
                    it.counterpartyAddress
                ]
            } == [
                [
                    CashOutSignalKind.STABLECOIN_SWAP,
                    SWAP_TX,
                    4000,
                    'USDC',
                    PAIR
                ],
                [
                    CashOutSignalKind.EXCHANGE,
                    EXIT_TX,
                    3500,
                    'USDC',
                    KRAKEN
                ]
            ]
            cashOutSignals[1].counterpartyLabel == ExclusionLabelDirectory.ETHERSCAN_SOURCE + ': Kraken 4 [kraken]'
            cashOutSignals*.transactionUrl == [
                CHAIN.url("/tx/$SWAP_TX"),
                CHAIN.url("/tx/$EXIT_TX")
            ]
        }
    }

    def "an explorer that does not answer gives unavailable activity after its first request"() {
        when:
        def outcome = collect(attribution(network, address))

        then:
        outcome.status == AddressActivityStatus.UNAVAILABLE
        outcome.activity == null
        CHAIN.requests == [firstRequest]

        where:
        network | address | firstRequest
        'BTC'   | BTC     | "explorer /api/address/$BTC".toString()
        'ETH'   | ETH     | "explorer /api/v2/addresses/$ETH/counters".toString()
    }

    def "demo addresses come from the fixture and an Attribution at 0% is not checked: nothing reaches the explorers"() {
        given:
        def demo = attribution('ETH', DEMO_ETH, 95)
        def unlinked = attribution('BTC', BTC, 0)

        when:
        def outcomes = collector.collect([demo, unlinked])

        then:
        outcomes.keySet() == [demo.candidate] as Set
        outcomes[demo.candidate].activity.synthetic
        CHAIN.requests.isEmpty()
    }

    def "in a demo Analysis only the unclassified contract goes to the explorer; its unavailable activity makes the Analysis partial"() {
        given:
        def caseId = createCase([nickname: 'zenek_fx'])
        def started = new JsonSlurper().parseText(client.post().uri("/api/1/cases/${caseId}/analyses").exchange()
                .expectStatus().isAccepted().expectBody(String).returnResult().responseBody)

        when:
        def report = finishedReport(started.id as String)

        then:
        report.attributions.collectEntries {
            [(it.address): it.addressActivityStatus]
        } == [
            '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'        : 'AVAILABLE',
            (DEMO_ETH)                                  : 'AVAILABLE',
            (CONTRACT)                                  : 'UNAVAILABLE',
            'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4': 'AVAILABLE',
        ]
        report.attributions.find {
            it.address == CONTRACT
        }.addressActivity == null
        report.limitations.contains('BLOCKCHAIN_DATA_UNAVAILABLE')
        CHAIN.requests == [
            "explorer /api/v2/addresses/$CONTRACT/counters".toString()
        ]
    }
}
