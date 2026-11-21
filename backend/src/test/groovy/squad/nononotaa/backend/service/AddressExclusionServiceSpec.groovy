package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.ExclusionReason.EXCHANGE
import static squad.nononotaa.backend.model.ExclusionReason.FUNDRAISER
import static squad.nononotaa.backend.model.ExclusionReason.MIXER
import static squad.nononotaa.backend.model.ExclusionReason.SMART_CONTRACT
import static squad.nononotaa.backend.model.Network.BTC
import static squad.nononotaa.backend.model.Network.ETH

import spock.lang.Specification
import squad.nononotaa.backend.model.AddressCandidate
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.ExcludedAddress
import squad.nononotaa.backend.model.KnownExcludedAddress

class AddressExclusionServiceSpec extends Specification {

    static final PERSONAL_ETH = candidate(ETH, '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed', 'm1')
    static final EXCHANGE_ETH = candidate(ETH, '0x28c6c06298d514db089934071355e5743bf21d60', 'm2')
    static final PERSONAL_BTC = candidate(BTC, 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4', 'm3')

    def ownList = new OwnListClassificationSource(new AddressValidator())

    def "classifies exchanges, smart contracts, mixers and fundraisers as Excluded Addresses with reason and source"() {
        given:
        def candidate = candidate(network, address, 'm1')

        when:
        def result = new AddressExclusionService([ownList]).classify([candidate])

        then:
        result.excluded == [
            new ExcludedAddress(candidate, reason, "Own list v1: $label")
        ]
        result.accepted.empty
        result.unavailable.empty

        where:
        network | address                                                          | reason         | label
        ETH     | '0x28c6c06298d514db089934071355e5743bf21d60'                     | EXCHANGE       | 'Binance 14 (hot wallet)'
        BTC     | 'bc1qgdjqv0av3q56jvd82tkdjpy7gdp9ut8tlqmgrpmv24sq90ecnvqqjwvw97' | EXCHANGE       | 'Bitfinex (cold wallet)'
        ETH     | '0xdac17f958d2ee523a2206206994597c13d831ec7'                     | SMART_CONTRACT | 'Tether USD (USDT) token contract'
        ETH     | '0x910cbd523d972eb0a6f4cae4618ad62622b39dbf'                     | MIXER          | 'Tornado Cash 10 ETH pool'
        ETH     | '0x165cd37b4c644c2921454429e7f9358d18a45e14'                     | FUNDRAISER     | 'Ukraine government donations'
    }

    def "an Excluded Address is not among the candidates handed to the Confidence Level algorithm"() {
        when:
        def result = new AddressExclusionService([ownList]).classify([
            PERSONAL_ETH,
            EXCHANGE_ETH,
            PERSONAL_BTC
        ])

        then:
        result.accepted == [PERSONAL_ETH, PERSONAL_BTC]
        result.excluded*.candidate == [EXCHANGE_ETH]
        result.unavailable.empty
        result.checkedSources == ['Own list v1']
        result.unavailableSources.empty
    }

    def "an unavailable source makes unlisted candidates unavailable, never accepted or excluded"() {
        given:
        def failing = source('Tags API', [ETH] as Set) {
            throw new ClassificationUnavailableException('Tags API', new IOException())
        }

        when:
        def result = new AddressExclusionService([ownList, failing]).classify([
            PERSONAL_ETH,
            EXCHANGE_ETH,
            PERSONAL_BTC
        ])

        then: 'a confirmed exclusion by a source that answered stays confirmed'
        result.excluded*.candidate == [EXCHANGE_ETH]

        and: 'the failing source only covers ETH, so the BTC candidate is fully classified'
        result.accepted == [PERSONAL_BTC]
        result.unavailable == [PERSONAL_ETH]
        result.checkedSources == ['Own list v1']
        result.unavailableSources == ['Tags API']
    }

    def "without any applicable source the classification is unavailable, not a fabricated result"() {
        when:
        def result = new AddressExclusionService(sources).classify([PERSONAL_BTC])

        then:
        result.unavailable == [PERSONAL_BTC]
        result.accepted.empty
        result.excluded.empty

        where:
        sources << [
            [],
            [
                source('ETH only', [ETH] as Set) { [:] }
            ]
        ]
    }

    def "the first source that lists the address gives the reason and the source"() {
        given:
        def other = source('Other list', [ETH] as Set) {
            [(EXCHANGE_ETH.canonicalAddress): new KnownExcludedAddress(MIXER, 'Other label')]
        }

        when:
        def result = new AddressExclusionService([ownList, other]).classify([EXCHANGE_ETH])

        then:
        result.excluded == [
            new ExcludedAddress(EXCHANGE_ETH, EXCHANGE, 'Own list v1: Binance 14 (hot wallet)')
        ]
    }

    def "a source is asked only about addresses of the networks it covers"() {
        given:
        def asked = []
        def ethOnly = source('ETH only', [ETH] as Set) { addresses ->
            asked.addAll(addresses); [:]
        }

        when:
        new AddressExclusionService([ethOnly]).classify([PERSONAL_ETH, PERSONAL_BTC])

        then:
        asked == [PERSONAL_ETH.canonicalAddress]
    }

    def "an empty candidate list gives an empty result"() {
        expect:
        with(new AddressExclusionService([ownList]).classify([])) {
            accepted.empty && excluded.empty && unavailable.empty
        }
    }

    private static AddressCandidate candidate(network, String address, String mentionId) {
        new AddressCandidate(new CanonicalAddress(network, address), [mentionId])
    }

    private AddressClassificationSource source(String name, Set networks, Closure answer) {
        Stub(AddressClassificationSource) {
            getName() >> name
            getNetworks() >> networks
            findExcluded(_) >> { args -> answer(args[0]) }
        }
    }
}
