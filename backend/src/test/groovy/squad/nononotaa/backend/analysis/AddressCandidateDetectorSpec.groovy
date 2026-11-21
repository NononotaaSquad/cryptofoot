package squad.nononotaa.backend.analysis

import spock.lang.Specification
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.RawAddressCandidate

class AddressCandidateDetectorSpec extends Specification {

    def detector = new AddressCandidateDetector()

    def "detects a BTC base58 address with BTC network hint"() {
        expect:
        detector.detect('m1', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa') ==
                [
                    new RawAddressCandidate('m1', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa', Network.BTC)
                ]
    }

    def "detects a BTC bech32 address with BTC network hint"() {
        expect:
        detector.detect('m1', 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4') ==
                [
                    new RawAddressCandidate('m1', 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4', Network.BTC)
                ]
    }

    def "detects an EVM address with ETH network hint"() {
        expect:
        detector.detect('m1', '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed') ==
                [
                    new RawAddressCandidate('m1', '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed', Network.ETH)
                ]
    }

    def "detects addresses embedded in a sentence"() {
        expect:
        detector.detect('m1', 'pay me at 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa or 0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed, thanks!') ==
                [
                    new RawAddressCandidate('m1', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa', Network.BTC),
                    new RawAddressCandidate('m1', '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed', Network.ETH),
                ]
    }

    def "does not detect a 64-char transaction hash that starts like a base58 address"() {
        expect:
        detector.detect('m1', 'tx 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa confirmed') == []
    }

    def "does not detect an EVM-like string with 41 hex digits"() {
        expect:
        detector.detect('m1', '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAedA') == []
    }

    def "does not detect a base58-like string glued to a word"() {
        expect:
        detector.detect('m1', 'id1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa') == []
    }

    def "detects a BTC P2SH base58 address starting with 3"() {
        expect:
        detector.detect('m1', '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy') ==
                [
                    new RawAddressCandidate('m1', '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy', Network.BTC)
                ]
    }

    def "detects a bech32m (taproot-style) address as BTC"() {
        expect:
        detector.detect('m1', 'bc1paaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa') ==
                [
                    new RawAddressCandidate('m1', 'bc1paaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', Network.BTC)
                ]
    }

    def "does not detect a base58 near-miss containing an excluded character"() {
        expect:
        detector.detect('m1', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfN0') == []
    }

    def "does not detect a bech32 near-miss with a character outside the bech32 charset"() {
        expect:
        detector.detect('m1', 'bc1bw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4') == []
    }

    def "detects an all-uppercase or mixed-case bech32 candidate and leaves its validity to the validator"() {
        expect:
        detector.detect('m1', 'pay BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4 or bc1QW508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4')*.rawAddress == [
            'BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4',
            'bc1QW508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'
        ]
    }

    def "does not detect an EVM near-miss with 39 hex digits"() {
        expect:
        detector.detect('m1', '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAe') == []
    }

    def "does not detect random hex without the 0x prefix or an uppercase 0X prefix"() {
        expect:
        detector.detect('m1', '5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed 0X5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed') == []
    }

    def "returns an empty list for text without addresses and for empty text"() {
        expect:
        detector.detect('m1', 'just a normal forum post about crypto') == []
        detector.detect('m1', '') == []
    }

    def "detects multiple addresses in one text in order of appearance"() {
        expect:
        detector.detect('m1', 'first 0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed then bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4 then 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa') ==
                [
                    new RawAddressCandidate('m1', '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed', Network.ETH),
                    new RawAddressCandidate('m1', 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4', Network.BTC),
                    new RawAddressCandidate('m1', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa', Network.BTC),
                ]
    }
}
