package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.AddressValidationFailure.CHECKSUM_MISMATCH
import static squad.nononotaa.backend.model.Network.BTC
import static squad.nononotaa.backend.model.Network.ETH

import spock.lang.Specification
import squad.nononotaa.backend.model.AddressCandidate
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.RawAddressCandidate
import squad.nononotaa.backend.model.RejectedAddressCandidate

class AddressCandidateServiceSpec extends Specification {

    static final ETH_CHECKSUMMED = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'
    static final ETH_CANONICAL = new CanonicalAddress(ETH, '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed')
    static final BTC_UPPER = 'BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4'
    static final BTC_CANONICAL = new CanonicalAddress(BTC, 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4')

    def service = new AddressCandidateService(new AddressValidator())

    def "groups Mentions of the same canonical address into one candidate"() {
        when:
        def result = service.prepare([
            new RawAddressCandidate('m2', ETH_CHECKSUMMED, null),
            new RawAddressCandidate('m1', ETH_CANONICAL.address, ETH),
            new RawAddressCandidate('m3', BTC_UPPER, null),
            new RawAddressCandidate('m1', BTC_CANONICAL.address, null),
        ])

        then:
        result.candidates == [
            new AddressCandidate(BTC_CANONICAL, ['m1', 'm3']),
            new AddressCandidate(ETH_CANONICAL, ['m1', 'm2']),
        ]
        result.rejected.empty
    }

    def "counts a Mention once even when it repeats the address"() {
        when:
        def result = service.prepare([
            new RawAddressCandidate('m1', ETH_CHECKSUMMED, null),
            new RawAddressCandidate('m1', ETH_CANONICAL.address, null),
        ])

        then:
        result.candidates == [
            new AddressCandidate(ETH_CANONICAL, ['m1'])
        ]
    }

    def "keeps a malformed candidate out of the candidates and reports why"() {
        given:
        def typo = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAeD'

        when:
        def result = service.prepare([
            new RawAddressCandidate('m1', typo, null),
            new RawAddressCandidate('m2', ETH_CHECKSUMMED, null),
        ])

        then:
        result.candidates == [
            new AddressCandidate(ETH_CANONICAL, ['m2'])
        ]
        result.rejected == [
            new RejectedAddressCandidate('m1', typo, CHECKSUM_MISMATCH)
        ]
    }

    def "gives the same result regardless of input order"() {
        given:
        def raw = [
            new RawAddressCandidate('m1', ETH_CHECKSUMMED, null),
            new RawAddressCandidate('m2', BTC_UPPER, null),
            new RawAddressCandidate('m3', ETH_CANONICAL.address, null),
        ]

        expect:
        service.prepare(raw) == service.prepare(raw.reverse())
    }

    def "returns nothing for no raw candidates"() {
        when:
        def result = service.prepare([])

        then:
        result.candidates.empty
        result.rejected.empty
    }
}
