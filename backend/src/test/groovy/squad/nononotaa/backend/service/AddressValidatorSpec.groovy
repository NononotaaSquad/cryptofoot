package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.AddressValidationFailure.CHECKSUM_MISMATCH
import static squad.nononotaa.backend.model.AddressValidationFailure.MALFORMED
import static squad.nononotaa.backend.model.AddressValidationFailure.NETWORK_MISMATCH
import static squad.nononotaa.backend.model.AddressValidationFailure.UNSUPPORTED_NETWORK
import static squad.nononotaa.backend.model.Network.BTC
import static squad.nononotaa.backend.model.Network.ETH

import spock.lang.Specification
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.CanonicalAddress

class AddressValidatorSpec extends Specification {

    def validator = new AddressValidator()

    def "accepts a valid #description and returns its canonical form"() {
        expect:
        validator.validate(raw, null) == new AddressValidation.Valid(new CanonicalAddress(network, canonical))

        where:
        description                     | raw                                                                | network | canonical
        'BTC P2PKH'                     | '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'                               | BTC     | '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
        'BTC P2SH'                      | '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy'                               | BTC     | '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy'
        'BTC P2WPKH in upper case'      | 'BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4'                       | BTC     | 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'
        'BTC P2WSH'                     | 'bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3'   | BTC     | 'bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3'
        'BTC taproot'                   | 'bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0'   | BTC     | 'bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0'
        'BTC with surrounding spaces'   | ' 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa '                             | BTC     | '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
        'ETH EIP-55 checksum'           | '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'                       | ETH     | '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
        'ETH EIP-55 checksum (2)'       | '0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359'                       | ETH     | '0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359'
        'ETH EIP-55 checksum (3)'       | '0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB'                       | ETH     | '0xdbf03b407c01e7cd3cbea99509d93f8dddc8c6fb'
        'ETH EIP-55 checksum (4)'       | '0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb'                       | ETH     | '0xd1220a0cf47c7b9be7a2e6ba89f429762e7b9adb'
        'ETH all lower case'            | '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'                       | ETH     | '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
        'ETH all upper case'            | '0x5AAEB6053F3E94C9B9A09F33669435E7EF1BEAED'                       | ETH     | '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
    }

    def "rejects #description as #failure"() {
        expect:
        validator.validate(raw, null) == new AddressValidation.Invalid(failure)

        where:
        description                        | raw                                                                                  | failure
        'BTC Base58 with a typo'           | '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb'                                                 | CHECKSUM_MISMATCH
        'BTC Base58 too short'             | '1A1zP1eP5QGefi2DMPTfTL5SL'                                                          | MALFORMED
        'BTC Base58 with a forbidden char' | '1A1zP1eP5QGefi2DMPTfTL5SLmv7Div0Na'                                                 | MALFORMED
        'bech32 with a typo'               | 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5'                                         | CHECKSUM_MISMATCH
        'bech32 in mixed case'             | 'bc1QW508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'                                         | MALFORMED
        'witness v0 with bech32m'          | 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kemeawh'                                         | CHECKSUM_MISMATCH
        'witness v1 with bech32'           | 'bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqh2y7hd'                     | CHECKSUM_MISMATCH
        'bech32 with an invalid char'      | 'bc1p38j9r5y49hruaue7wxjce0updqjuyyx0kh56v8s25huc6995vvpql3jow4'                     | MALFORMED
        'witness version 17'               | 'BC130XLXVLHEMJA6C4DQV22UAPCTQUPFHLXM9H8Z3K2E72Q4K9HCZ7VQ7ZWS8R'                     | MALFORMED
        'witness program of 1 byte'        | 'bc1pw5dgrnzv'                                                                       | MALFORMED
        'witness program of 41 bytes'      | 'bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7v8n0nx0muaewav253zgeav'       | MALFORMED
        'v0 program of 16 bytes'           | 'BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P'                                               | MALFORMED
        'empty data part'                  | 'bc1gmk9yu'                                                                          | MALFORMED
        'BTC testnet bech32'               | 'tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx'                                         | UNSUPPORTED_NETWORK
        'BTC testnet Base58'               | 'mipcBbFg9gMiCh81Kj8tqqdgoZub1ZJRfn'                                                 | UNSUPPORTED_NETWORK
        'ETH with a wrong checksum'        | '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAeD'                                         | CHECKSUM_MISMATCH
        'ETH too short'                    | '0x5aaeb6053f3e94c9b9a09f33669435e7ef1bea'                                           | MALFORMED
        'ETH with a non-hex char'          | '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaeg'                                         | MALFORMED
        'TRON address'                     | 'TLa2f6VPqDgRE67v1736s7bJ8Ray5wYjU7'                                                 | UNSUPPORTED_NETWORK
        'plain text'                       | 'hello'                                                                              | UNSUPPORTED_NETWORK
        'empty string'                     | ''                                                                                   | UNSUPPORTED_NETWORK
    }

    def "rejects a valid address whose network contradicts the hint"() {
        expect:
        validator.validate('1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa', ETH) == new AddressValidation.Invalid(NETWORK_MISMATCH)
    }

    def "accepts a valid address whose network matches the hint"() {
        expect:
        validator.validate('0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed', ETH) instanceof AddressValidation.Valid
    }
}
