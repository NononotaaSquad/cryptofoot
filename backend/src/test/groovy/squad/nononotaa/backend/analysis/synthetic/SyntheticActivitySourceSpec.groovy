package squad.nononotaa.backend.analysis.synthetic

import java.time.Instant
import spock.lang.Shared
import spock.lang.Specification
import squad.nononotaa.backend.model.AddressActivityCoverageLimitation
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.CashOutSignalKind
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.service.AddressActivityEvaluator
import squad.nononotaa.backend.service.ExclusionLabelDirectory
import tools.jackson.module.kotlin.ExtensionsKt

// frozen expected Address Activity of the demo-v1 person (D23-007, E7-D2)
class SyntheticActivitySourceSpec extends Specification {

    static final CanonicalAddress ETH = new CanonicalAddress(Network.ETH, '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed')
    static final CanonicalAddress BTC = new CanonicalAddress(Network.BTC, '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa')
    static final CanonicalAddress BECH32 = new CanonicalAddress(Network.BTC, 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4')
    static final Instant CHECKED_AT = Instant.parse('2026-11-21T12:00:00Z')

    @Shared
    def mapper = ExtensionsKt.jacksonObjectMapper()

    @Shared
    def source = SyntheticActivitySource.load(mapper)

    @Shared
    def evaluator = new AddressActivityEvaluator(ExclusionLabelDirectory.load(
            mapper,
            SyntheticActivitySourceSpec.getResourceAsStream('/exclusion-labels/etherscan-labels.json'),
            SyntheticActivitySourceSpec.getResourceAsStream('/exclusion-labels/own-labels.json')), 1000 as BigDecimal)

    private evaluate(CanonicalAddress address) {
        evaluator.evaluate(address, source, source.fetch(address), CHECKED_AT)
    }

    def "the fixture is versioned, marked synthetic and covers only the three demo Attributions"() {
        expect:
        source.fixture.version == 'demo-v1'
        source.fixture.synthetic
        source.sourceName == 'synthetic'
        source.synthetic
        [ETH, BTC, BECH32].every { source.covers(it) }
        !source.covers(new CanonicalAddress(Network.ETH, '0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae'))
        !source.covers(new CanonicalAddress(Network.BTC, '1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s'))
        source.fetch(new CanonicalAddress(Network.BTC, '1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s')) == null
    }

    def "ETH: a swap into stablecoins and a transfer to an exchange; the small swap stays below the threshold"() {
        when:
        def activity = evaluate(ETH)

        then:
        activity.synthetic
        activity.unit == 'ETH'
        activity.totalReceived == 10.2
        activity.totalSent == 8.1
        activity.transactionCount == 5
        activity.firstActivityAt == Instant.parse('2026-03-02T10:12:00Z')
        activity.lastActivityAt == Instant.parse('2026-09-30T19:22:00Z')
        activity.cashOutSignals.collect {
            [
                it.kind,
                it.occurredAt.toString(),
                it.amount,
                it.unit,
                it.counterpartyLabel
            ]
        } == [
            [
                CashOutSignalKind.STABLECOIN_SWAP,
                '2026-08-11T09:05:00Z',
                21480,
                'USDC',
                null
            ],
            [
                CashOutSignalKind.EXCHANGE,
                '2026-08-12T11:30:00Z',
                20000,
                'USDC',
                ExclusionLabelDirectory.ETHERSCAN_SOURCE + ': Kraken 4 [kraken]'
            ]
        ]
        activity.cashOutSignals.every { it.transactionUrl == null }
        activity.coverageLimitations == [
            AddressActivityCoverageLimitation.LABELLED_COUNTERPARTIES_ONLY
        ]
    }

    def "BTC 1A1z: activity without any cash-out signal"() {
        when:
        def activity = evaluate(BTC)

        then:
        activity.unit == 'BTC'
        activity.totalReceived == 0.75
        activity.totalSent == 0.25
        activity.transactionCount == 4
        activity.firstActivityAt == Instant.parse('2026-01-15T08:00:00Z')
        activity.lastActivityAt == Instant.parse('2026-07-07T07:15:00Z')
        activity.cashOutSignals.isEmpty()
    }

    def "BTC bc1q (the P2P ad): a transfer to a P2P platform"() {
        when:
        def activity = evaluate(BECH32)

        then:
        activity.totalReceived == 1.2
        activity.totalSent == 1.1
        activity.transactionCount == 3
        activity.cashOutSignals.size() == 1
        with(activity.cashOutSignals[0]) {
            kind == CashOutSignalKind.P2P
            occurredAt == Instant.parse('2026-10-05T09:40:00Z')
            amount == 1.1
            unit == 'BTC'
            counterpartyAddress == 'bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3'
            counterpartyLabel == 'Synthetic fixture demo-v1: P2P marketplace escrow (synthetic)'
        }
    }
}
