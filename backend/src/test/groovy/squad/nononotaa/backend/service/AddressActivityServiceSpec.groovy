package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.AddressActivityStatus.AVAILABLE
import static squad.nononotaa.backend.model.AddressActivityStatus.UNAVAILABLE

import spock.lang.Specification
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.Network

class AddressActivityServiceSpec extends Specification {

    static final String ETH = '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
    static final String BTC = 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'

    def evaluator = new AddressActivityEvaluator(new ExclusionLabelDirectory([:]), 1000 as BigDecimal)
    def history = new ChainActivity('ETH', [], true, null, null, null, null, [])

    AddressActivitySource fixture = Mock() {
        getSourceName() >> 'synthetic'
        getSynthetic() >> true
    }
    AddressActivitySource explorer = Mock() {
        getSourceName() >> 'explorer'
        getSynthetic() >> false
    }

    private static ScoredAttribution attribution(String network, String address, int level) {
        new ScoredAttribution(new Candidate(network, address, ['m1']), level, [])
    }

    def "only Attributions with a Confidence Level above 0 are checked"() {
        given:
        def service = new AddressActivityService([explorer], evaluator)
        def linked = attribution('ETH', ETH, 10)
        def unlinked = attribution('BTC', BTC, 0)

        when:
        def outcomes = service.collect([linked, unlinked])

        then:
        1 * explorer.covers(new CanonicalAddress(Network.ETH, ETH)) >> true
        1 * explorer.fetch(new CanonicalAddress(Network.ETH, ETH)) >> history
        0 * explorer.covers(new CanonicalAddress(Network.BTC, BTC))
        outcomes.keySet() == [linked.candidate] as Set
        outcomes[linked.candidate].status == AVAILABLE
        outcomes[linked.candidate].activity.sourceName == 'explorer'
    }

    def "the fixture answers for its own addresses and nothing reaches the live explorer"() {
        given:
        def service = new AddressActivityService([fixture, explorer], evaluator)
        def demo = attribution('ETH', ETH, 95)

        when:
        def outcome = service.collect([demo])[demo.candidate]

        then:
        fixture.covers(_) >> true
        1 * fixture.fetch(_) >> history
        0 * explorer.fetch(_)
        outcome.status == AVAILABLE
        outcome.activity.synthetic
    }

    def "an address outside the fixture goes to the live explorer"() {
        given:
        def service = new AddressActivityService([fixture, explorer], evaluator)
        def found = attribution('ETH', ETH, 60)

        when:
        def outcome = service.collect([found])[found.candidate]

        then:
        fixture.covers(_) >> false
        explorer.covers(_) >> true
        0 * fixture.fetch(_)
        1 * explorer.fetch(_) >> history
        outcome.activity.sourceName == 'explorer'
    }

    def "a source that gives no answer or fails is unavailable, never zero activity"() {
        given:
        def service = new AddressActivityService(withExplorer ? [explorer] : [], evaluator)
        def found = attribution('ETH', ETH, 60)
        explorer.covers(_) >> true
        explorer.fetch(_) >> answer

        expect:
        service.collect([found])[found.candidate].with {
            it.status == UNAVAILABLE && it.activity == null
        }

        where:
        withExplorer | answer
        false        | null
        true         | null
        true         | { throw new IllegalStateException('parse error') }
    }

    def "an Attribution of an unsupported network is unavailable"() {
        given:
        def service = new AddressActivityService([explorer], evaluator)
        def odd = attribution('SOL', 'abc', 60)

        expect:
        service.collect([odd])[odd.candidate].status == UNAVAILABLE
    }
}
