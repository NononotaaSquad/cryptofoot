package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.ContractCodeCheck.ACCOUNT
import static squad.nononotaa.backend.model.ContractCodeCheck.CONTRACT
import static squad.nononotaa.backend.model.ContractCodeCheck.UNAVAILABLE
import static squad.nononotaa.backend.model.ExclusionReason.EXCHANGE
import static squad.nononotaa.backend.model.ExclusionReason.FUNDRAISER
import static squad.nononotaa.backend.model.ExclusionReason.MIXER
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
    static final String BTC_MIXER = 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'

    def labels = new ExclusionLabelDirectory([
        (new CanonicalAddress(ETH, KRAKEN))      : new ExclusionLabel(EXCHANGE, 'Kraken 4 [kraken]', 'Etherscan labels'),
        (new CanonicalAddress(BTC, BTC_DONATION)): new ExclusionLabel(FUNDRAISER, 'Ukraine Crypto Donation (BTC)', 'Own label list (ref)'),
        (new CanonicalAddress(BTC, BTC_MIXER))   : new ExclusionLabel(MIXER, 'Test mixer', 'Own label list (ref)')
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

    def "accepts an ETH account confirmed by the fallback service"() {
        given:
        def person = new Candidate('ETH', ETH_ADDRESS, ['m1'])

        when:
        def result = classifier.classify([person])

        then:
        1 * rpc.check(ETH_ADDRESS) >> UNAVAILABLE
        1 * blockchair.check(ETH_ADDRESS) >> ACCOUNT
        result == new ExclusionResult([person], [], [])
    }

    def "a bech32 label matches regardless of letter case"() {
        given:
        def mixer = new Candidate('BTC', BTC_MIXER.toUpperCase(), ['m1'])

        expect:
        classifier.classify([mixer]).excluded == [
            new ExcludedCandidate(mixer, MIXER, 'Own label list (ref): Test mixer')
        ]
    }

    def "a Base58 label is case-sensitive, so a different spelling is not that address"() {
        given:
        def other = new Candidate('BTC', BTC_DONATION.toLowerCase(), ['m1'])

        expect:
        classifier.classify([other]) == new ExclusionResult([other], [], [])
    }

    def "surrounding spaces are ignored and the on-chain check gets the canonical address"() {
        given:
        def listed = new Candidate('ETH', "  ${KRAKEN.toUpperCase().replace('0X', '0x')}  ".toString(), ['m1'])
        def unlisted = new Candidate('ETH', " 0x${ETH_ADDRESS.substring(2).toUpperCase()} ".toString(), ['m2'])

        when:
        def result = classifier.classify([listed, unlisted])

        then:
        1 * rpc.check(ETH_ADDRESS) >> ACCOUNT
        result.excluded*.candidate == [listed]
        result.accepted == [unlisted]
    }

    def "network names must match exactly: #network"() {
        given:
        def candidate = new Candidate(network, ETH_ADDRESS, ['m1'])

        when:
        def result = classifier.classify([candidate])

        then:
        0 * rpc.check(_)
        result == new ExclusionResult([], [], [candidate])

        where:
        network << ['eth', 'ETHEREUM', '', 'BTC ']
    }

    def "no candidates give an empty result without any call"() {
        when:
        def result = classifier.classify([])

        then:
        0 * rpc.check(_)
        0 * blockchair.check(_)
        result == new ExclusionResult([], [], [])
    }

    def "every candidate lands in exactly one list and an exclusion keeps all its Mentions"() {
        given:
        def candidates = [
            new Candidate('ETH', KRAKEN, ['m1', 'm2', 'm3']),
            new Candidate('ETH', ETH_ADDRESS, ['m4']),
            new Candidate('ETH', '0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359', ['m5']),
            new Candidate('ETH', '0xdbf03b407c01e7cd3cbea99509d93f8dddc8c6fb', ['m6']),
            new Candidate('BTC', BTC_PERSON, ['m7']),
            new Candidate('BTC', BTC_MIXER, ['m8', 'm9']),
            new Candidate('TRON', 'TXYZ', ['m10'])
        ]
        rpc.check(ETH_ADDRESS) >> CONTRACT
        rpc.check('0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359') >> ACCOUNT
        rpc.check('0xdbf03b407c01e7cd3cbea99509d93f8dddc8c6fb') >> UNAVAILABLE
        blockchair.check(_) >> UNAVAILABLE

        when:
        def result = classifier.classify(candidates)

        then:
        (result.accepted + result.excluded*.candidate + result.unclassified) as Set == candidates as Set
        result.accepted.size() + result.excluded.size() + result.unclassified.size() == candidates.size()
        result.excluded.collectEntries {
            [(it.candidate.address): [
                    it.reason,
                    it.candidate.mentionIds
                ]]
        } == [
            (KRAKEN)     : [EXCHANGE, ['m1', 'm2', 'm3']],
            (ETH_ADDRESS): [SMART_CONTRACT, ['m4']],
            (BTC_MIXER)  : [MIXER, ['m8', 'm9']]
        ]
        result.accepted*.address == [
            '0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359',
            BTC_PERSON
        ]
        result.unclassified*.address == [
            '0xdbf03b407c01e7cd3cbea99509d93f8dddc8c6fb',
            'TXYZ'
        ]
    }
}
