package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.AddressActivityCoverageLimitation.INTERNAL_TRANSFERS_NOT_COUNTED
import static squad.nononotaa.backend.model.AddressActivityCoverageLimitation.LABELLED_COUNTERPARTIES_ONLY
import static squad.nononotaa.backend.model.AddressActivityCoverageLimitation.RECENT_TRANSACTIONS_ONLY
import static squad.nononotaa.backend.model.TransferDirection.IN
import static squad.nononotaa.backend.model.TransferDirection.OUT

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.CashOutSignalKind
import squad.nononotaa.backend.model.ExclusionLabel
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.Network

class AddressActivityEvaluatorSpec extends Specification {

    static final String OWNER = '0xd1220a0cf47c7b9be7a2e6ba89f429762e7b9adb'
    static final String KRAKEN = '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0'
    static final String TORNADO = '0x722122df12d4e14e13ac3b6895a86e84145b6967'
    static final String STRANGER = '0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359'
    static final String ROUTER = '0x7a250d5630b4cf539739df2c5dacb4c659f2488d'
    static final String USDC = '0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48'
    static final String USDT = '0xdac17f958d2ee523a2206206994597c13d831ec7'
    static final String FAKE_USDC = '0x1111111111111111111111111111111111111111'
    static final Instant T0 = Instant.parse('2026-03-01T10:00:00Z')
    static final Instant CHECKED_AT = Instant.parse('2026-11-21T12:00:00Z')

    def labels = new ExclusionLabelDirectory([
        (new CanonicalAddress(Network.ETH, KRAKEN)) : new ExclusionLabel(ExclusionReason.EXCHANGE, 'Kraken 4 [kraken]', 'Etherscan labels'),
        (new CanonicalAddress(Network.ETH, TORNADO)): new ExclusionLabel(ExclusionReason.MIXER, 'Tornado Cash', 'Etherscan labels'),
    ])
    def evaluator = new AddressActivityEvaluator(labels, 1000 as BigDecimal)
    def owner = new CanonicalAddress(Network.ETH, OWNER)
    AddressActivitySource source = Stub() {
        getSourceName() >> 'test explorer'
        getSynthetic() >> false
    }

    private static ChainTransfer transfer(Map args) {
        new ChainTransfer(args.id ?: 'tx1', args.at ?: T0, args.direction ?: IN, (args.amount ?: 1G) as BigDecimal, args.unit ?: 'ETH',
                args.token as String, args.counterparty as String, args.label as OffRampLabel)
    }

    private static ChainActivity chain(List<ChainTransfer> transfers, Map args = [:]) {
        new ChainActivity('ETH', transfers, args.get('complete', true) as boolean, args.received as BigDecimal, args.sent as BigDecimal,
                args.count as Integer, args.urlPrefix as String, args.get('limitations', []) as List)
    }

    private evaluate(ChainActivity chain, CanonicalAddress address = owner) {
        evaluator.evaluate(address, source, chain, CHECKED_AT)
    }

    def "a complete history gives totals of the coin, the transaction count and the period"() {
        when:
        def activity = evaluate(chain([
            transfer(id: 'a', at: T0, direction: IN, amount: 4.2G),
            transfer(id: 'b', at: T0.plusSeconds(3600), direction: IN, amount: 6G),
            transfer(id: 'c', at: T0.plusSeconds(7200), direction: OUT, amount: 8.1G, counterparty: STRANGER),
            transfer(id: 'c', at: T0.plusSeconds(7200), direction: IN, amount: 500G, unit: 'USDC', token: USDC),
        ]))

        then:
        activity.sourceName == 'test explorer'
        !activity.synthetic
        activity.checkedAt == CHECKED_AT
        activity.unit == 'ETH'
        activity.totalReceived == 10.2G
        activity.totalSent == 8.1G
        activity.transactionCount == 3
        activity.firstActivityAt == T0
        activity.lastActivityAt == T0.plusSeconds(7200)
        activity.coverageLimitations == [LABELLED_COUNTERPARTIES_ONLY]
    }

    def "a history with no transfers is real zero activity, not missing data"() {
        when:
        def activity = evaluate(chain([]))

        then:
        activity.totalReceived == 0G
        activity.totalSent == 0G
        activity.transactionCount == 0
        activity.firstActivityAt == null
        activity.lastActivityAt == null
        activity.cashOutSignals.isEmpty()
    }

    def "only the most recent page leaves totals, count and first activity unknown instead of zero"() {
        when:
        def activity = evaluate(chain([
            transfer(id: 'a', at: T0, direction: IN, amount: 4G)
        ], [complete: false]))

        then:
        activity.totalReceived == null
        activity.totalSent == null
        activity.transactionCount == null
        activity.firstActivityAt == null
        activity.lastActivityAt == T0
        activity.coverageLimitations == [
            RECENT_TRANSACTIONS_ONLY,
            LABELLED_COUNTERPARTIES_ONLY
        ]
    }

    def "totals and the count stated by the source win over the page it returned"() {
        when:
        def activity = evaluate(chain([
            transfer(direction: IN, amount: 4G)
        ], [complete: false, received: 0.75G, sent: 0.25G, count: 40, limitations: [
                INTERNAL_TRANSFERS_NOT_COUNTED
            ]]))

        then:
        activity.totalReceived == 0.75G
        activity.totalSent == 0.25G
        activity.transactionCount == 40
        activity.coverageLimitations == [
            RECENT_TRANSACTIONS_ONLY,
            LABELLED_COUNTERPARTIES_ONLY,
            INTERNAL_TRANSFERS_NOT_COUNTED
        ]
    }

    def "an outgoing transfer to a listed exchange is a cash-out signal at any amount, with the list as provenance"() {
        when:
        def signals = evaluate(chain([
            transfer(id: 'dust', direction: OUT, amount: 0.0001G, counterparty: KRAKEN.toUpperCase().replace('0X', '0x'))
        ], [urlPrefix: 'https://explorer.example/tx/'])).cashOutSignals

        then:
        signals.size() == 1
        with(signals[0]) {
            kind == CashOutSignalKind.EXCHANGE
            transactionId == 'dust'
            transactionUrl == 'https://explorer.example/tx/dust'
            amount == 0.0001G
            unit == 'ETH'
            counterpartyAddress == KRAKEN.toUpperCase().replace('0X', '0x')
            counterpartyLabel == 'Etherscan labels: Kraken 4 [kraken]'
        }
    }

    def "money coming from an exchange, going to an unlisted address or to a mixer is not a cash-out signal"() {
        expect:
        evaluate(chain([
            transfer(id: 'a', direction: IN, counterparty: KRAKEN),
            transfer(id: 'b', direction: OUT, counterparty: STRANGER),
            transfer(id: 'c', direction: OUT, counterparty: TORNADO),
            transfer(id: 'd', direction: OUT, counterparty: null),
        ])).cashOutSignals.isEmpty()
    }

    def "a fixture label names other off-ramps, but a list label wins"() {
        given:
        def p2p = new OffRampLabel(CashOutSignalKind.P2P, 'P2P escrow', 'Synthetic fixture demo-v1')

        when:
        def signals = evaluate(chain([
            transfer(id: 'a', direction: OUT, counterparty: STRANGER, label: p2p),
            transfer(id: 'b', at: T0.plusSeconds(60), direction: OUT, counterparty: KRAKEN, label: p2p),
        ])).cashOutSignals

        then:
        signals*.kind == [
            CashOutSignalKind.P2P,
            CashOutSignalKind.EXCHANGE
        ]
        signals*.counterpartyLabel == [
            'Synthetic fixture demo-v1: P2P escrow',
            'Etherscan labels: Kraken 4 [kraken]'
        ]
    }

    def "a swap into a USD stablecoin is a cash-out signal from the threshold: #bought USDC"() {
        when:
        def signals = evaluate(chain([
            transfer(id: 'swap', direction: OUT, amount: 1G, counterparty: ROUTER),
            transfer(id: 'swap', direction: IN, amount: bought, unit: 'USDC', token: USDC, counterparty: ROUTER),
        ])).cashOutSignals

        then:
        signals*.kind == (signal ? [
            CashOutSignalKind.STABLECOIN_SWAP
        ] : [])
        signals*.amount == (signal ? [bought] : [])
        signals*.unit == (signal ? ['USDC'] : [])
        signals*.counterpartyLabel == (signal ? [null] : [])

        where:
        bought   | signal
        999.99G  | false
        1000G    | true
        21480G   | true
    }

    def "a look-alike token, a swap between stablecoins and a stablecoin received without selling are not swaps"() {
        expect:
        evaluate(chain([
            transfer(id: 'fake', direction: OUT, amount: 1G),
            transfer(id: 'fake', direction: IN, amount: 5000G, unit: 'USDC', token: FAKE_USDC),
            transfer(id: 'stable', direction: OUT, amount: 5000G, unit: 'USDT', token: USDT, counterparty: ROUTER),
            transfer(id: 'stable', direction: IN, amount: 5000G, unit: 'USDC', token: USDC),
            transfer(id: 'gift', direction: IN, amount: 5000G, unit: 'USDC', token: USDC),
        ])).cashOutSignals.isEmpty()
    }

    def "signals are in time order and Bitcoin has no stablecoin swaps"() {
        given:
        def btcExchange = '1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s'
        def btcLabels = new ExclusionLabelDirectory([
            (new CanonicalAddress(Network.BTC, btcExchange)): new ExclusionLabel(ExclusionReason.EXCHANGE, 'Binance.com', 'Own label list')
        ])
        def btc = new AddressActivityEvaluator(btcLabels, 1000 as BigDecimal)

        when:
        def signals = btc.evaluate(new CanonicalAddress(Network.BTC, '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'), source, chain([
            transfer(id: 'late', at: T0.plusSeconds(600), direction: OUT, unit: 'BTC', counterparty: btcExchange),
            transfer(id: 'early', at: T0, direction: OUT, unit: 'BTC', counterparty: btcExchange),
            transfer(id: 'early', at: T0, direction: IN, amount: 5000G, unit: 'USDC', token: USDC),
        ]), CHECKED_AT).cashOutSignals

        then:
        signals*.transactionId == ['early', 'late']
        signals*.kind == [
            CashOutSignalKind.EXCHANGE,
            CashOutSignalKind.EXCHANGE
        ]
    }
}
