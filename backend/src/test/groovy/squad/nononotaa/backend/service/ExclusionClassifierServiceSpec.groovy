package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.ContractCodeCheck.ACCOUNT
import static squad.nononotaa.backend.model.ContractCodeCheck.CONTRACT
import static squad.nononotaa.backend.model.ContractCodeCheck.UNAVAILABLE
import static squad.nononotaa.backend.model.ExclusionReason.EXCHANGE
import static squad.nononotaa.backend.model.ExclusionReason.FUNDRAISER
import static squad.nononotaa.backend.model.ExclusionReason.SMART_CONTRACT
import static squad.nononotaa.backend.model.Network.BTC
import static squad.nononotaa.backend.model.Network.ETH

import spock.lang.Specification
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.ExcludedCandidate
import squad.nononotaa.backend.analysis.ExclusionResult
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.ExclusionLabel

class ExclusionClassifierServiceSpec extends Specification {

    static final String KRAKEN = '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0'
    static final String BTC_DONATION = '357a3So9CbsNfBBgFYACGvxxS6tMaDoa1P'
    static final String ETH_ADDRESS = '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
    static final String BTC_PERSON = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'

    def labels = new ExclusionLabelDirectory([
        (new CanonicalAddress(ETH, KRAKEN))      : new ExclusionLabel(EXCHANGE, 'Kraken 4 [kraken]', 'Etherscan labels'),
        (new CanonicalAddress(BTC, BTC_DONATION)): new ExclusionLabel(FUNDRAISER, 'Ukraine Crypto Donation (BTC)', 'Own label list (ref)')
    ])
    ContractCodeSource rpc = Mock() {
        getSourceName() >> 'Ethereum JSON-RPC eth_getCode (rpc.test)'
    }
    ContractCodeSource blockchair = Mock() {
        getSourceName() >> 'Blockchair (blockchair.test)'
    }
    def classifier = new ExclusionClassifierService(labels, [rpc, blockchair])

    def "a labelled address is excluded with its reason and source and is never sent to an on-chain service"() {
        given:
        def exchange = new Candidate('ETH', KRAKEN, ['m1', 'm2'])
        def donation = new Candidate('BTC', BTC_DONATION, ['m3'])

        when:
        def result = classifier.classify([exchange, donation])

        then:
        0 * rpc.check(_)
        0 * blockchair.check(_)
        result == new ExclusionResult([], [
            new ExcludedCandidate(exchange, EXCHANGE, 'Etherscan labels: Kraken 4 [kraken]'),
            new ExcludedCandidate(donation, FUNDRAISER, 'Own label list (ref): Ukraine Crypto Donation (BTC)')
        ], [])
    }

    def "the label lookup ignores the letter case of an ETH address"() {
        when:
        def result = classifier.classify([
            new Candidate('ETH', '0x' + KRAKEN.substring(2).toUpperCase(), ['m1'])
        ])

        then:
        result.excluded*.reason == [EXCHANGE]
    }

    def "an ETH address with executable code is excluded as a smart contract with the checking service as source"() {
        given:
        def contract = new Candidate('ETH', ETH_ADDRESS, ['m1'])

        when:
        def result = classifier.classify([contract])

        then:
        1 * rpc.check(ETH_ADDRESS) >> CONTRACT
        0 * blockchair.check(_)
        result == new ExclusionResult([], [
            new ExcludedCandidate(contract, SMART_CONTRACT, 'Ethereum JSON-RPC eth_getCode (rpc.test): executable code present')
        ], [])
    }

    def "an ETH account without code is accepted"() {
        given:
        def person = new Candidate('ETH', ETH_ADDRESS, ['m1'])

        when:
        def result = classifier.classify([person])

        then:
        1 * rpc.check(ETH_ADDRESS) >> ACCOUNT
        0 * blockchair.check(_)
        result == new ExclusionResult([person], [], [])
    }

    def "falls back to the next service when the first one is unavailable"() {
        given:
        def contract = new Candidate('ETH', ETH_ADDRESS, ['m1'])

        when:
        def result = classifier.classify([contract])

        then:
        1 * rpc.check(ETH_ADDRESS) >> UNAVAILABLE
        1 * blockchair.check(ETH_ADDRESS) >> CONTRACT
        result.excluded*.classificationSource == [
            'Blockchair (blockchair.test): executable code present'
        ]
    }

    def "an ETH address is unclassified, neither accepted nor excluded, when every service is unavailable"() {
        given:
        def unknown = new Candidate('ETH', ETH_ADDRESS, ['m1'])

        when:
        def result = classifier.classify([unknown])

        then:
        1 * rpc.check(ETH_ADDRESS) >> UNAVAILABLE
        1 * blockchair.check(ETH_ADDRESS) >> UNAVAILABLE
        result == new ExclusionResult([], [], [unknown])
    }

    def "with the on-chain check switched off an unlisted ETH address is unclassified and a listed one still excluded"() {
        given:
        def offline = new ExclusionClassifierService(labels, [])
        def unknown = new Candidate('ETH', ETH_ADDRESS, ['m1'])
        def exchange = new Candidate('ETH', KRAKEN, ['m2'])

        expect:
        offline.classify([unknown, exchange]) == new ExclusionResult([], [
            new ExcludedCandidate(exchange, EXCHANGE, 'Etherscan labels: Kraken 4 [kraken]')
        ], [unknown])
    }

    def "an unlisted BTC address is accepted without any external call"() {
        given:
        def person = new Candidate('BTC', BTC_PERSON, ['m1'])

        when:
        def result = classifier.classify([person])

        then:
        0 * rpc.check(_)
        0 * blockchair.check(_)
        result == new ExclusionResult([person], [], [])
    }

    def "a malformed ETH candidate is unclassified and never sent to an on-chain service"() {
        given:
        def malformed = new Candidate('ETH', '0x123", "evil', ['m1'])

        when:
        def result = classifier.classify([malformed])

        then:
        0 * rpc.check(_)
        0 * blockchair.check(_)
        result == new ExclusionResult([], [], [malformed])
    }

    def "a candidate of an unsupported network is unclassified"() {
        given:
        def tron = new Candidate('TRON', 'TXYZ', ['m1'])

        expect:
        classifier.classify([tron]) == new ExclusionResult([], [], [tron])
    }

    def "keeps the input order and gives the same result for the same input"() {
        given:
        def candidates = [
            new Candidate('BTC', BTC_PERSON, ['m1']),
            new Candidate('ETH', KRAKEN, ['m2']),
            new Candidate('BTC', '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy', ['m3']),
            new Candidate('BTC', BTC_DONATION, ['m4'])
        ]

        when:
        def first = classifier.classify(candidates)

        then:
        first == classifier.classify(candidates)
        first.accepted*.address == [
            BTC_PERSON,
            '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy'
        ]
        first.excluded*.candidate*.address == [KRAKEN, BTC_DONATION]
    }
}
