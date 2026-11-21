package squad.nononotaa.backend.service

import spock.lang.Specification
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.MentionAddresses
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.RejectionReason

class AddressCandidateServiceSpec extends Specification {

    def service = new AddressCandidateService()

    def "accepts a valid address of its network in canonical form"() {
        when:
        def result = service.validate(value)

        then:
        result instanceof AddressCandidateService.Validation.Valid
        result.address == new CanonicalAddress(network, canonical)

        where:
        value                                                            | network          | canonical
        '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'                             | Network.BITCOIN  | '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
        '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy'                             | Network.BITCOIN  | '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy'
        'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'                     | Network.BITCOIN  | 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'
        'BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4'                     | Network.BITCOIN  | 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'
        'bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0' | Network.BITCOIN  | 'bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0'
        '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'                     | Network.ETHEREUM | '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
        '0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359'                     | Network.ETHEREUM | '0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359'
        '0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB'                     | Network.ETHEREUM | '0xdbf03b407c01e7cd3cbea99509d93f8dddc8c6fb'
        '0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb'                     | Network.ETHEREUM | '0xd1220a0cf47c7b9be7a2e6ba89f429762e7b9adb'
        '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'                     | Network.ETHEREUM | '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
        '  0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed '                  | Network.ETHEREUM | '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
    }

    def "rejects a candidate that only looks like an address"() {
        when:
        def result = service.validate(value)

        then:
        result == new AddressCandidateService.Validation.Invalid(network, reason)

        where:
        value                                         | network          | reason
        '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb'          | Network.BITCOIN  | RejectionReason.INVALID_ADDRESS
        '3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLz'          | Network.BITCOIN  | RejectionReason.INVALID_ADDRESS
        'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5'  | Network.BITCOIN  | RejectionReason.INVALID_ADDRESS
        'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kemeawh'  | Network.BITCOIN  | RejectionReason.INVALID_ADDRESS
        'bc1qW508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'  | Network.BITCOIN  | RejectionReason.INVALID_ADDRESS
        '0x5aaeb6053F3E94C9b9A09f33669435E7Ef1BeAed'  | Network.ETHEREUM | RejectionReason.INVALID_ADDRESS
        '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAeD'  | Network.ETHEREUM | RejectionReason.INVALID_ADDRESS
        'hello'                                       | null             | RejectionReason.UNKNOWN_FORMAT
        '0x123'                                       | null             | RejectionReason.UNKNOWN_FORMAT
        ''                                            | null             | RejectionReason.UNKNOWN_FORMAT
    }

    def "groups all Mentions with the same canonical address into one candidate"() {
        given:
        def mentions = [
            new MentionAddresses('m3', [
                '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
            ]),
            new MentionAddresses('m1', [
                '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed',
                '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
            ]),
            new MentionAddresses('m2', [
                '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed',
                '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'
            ]),
        ]

        when:
        def result = service.prepare(mentions)

        then:
        result.valid*.address == [
            new CanonicalAddress(Network.BITCOIN, '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'),
            new CanonicalAddress(Network.ETHEREUM, '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'),
        ]
        result.valid*.mentionIds == [['m1'], ['m1', 'm2', 'm3']]
        result.rejected.empty
    }

    def "gives the same candidates for the same Mentions in any order"() {
        given:
        def mentions = [
            new MentionAddresses('m1', [
                'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4',
                '0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359'
            ]),
            new MentionAddresses('m2', [
                'BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4'
            ]),
        ]

        expect:
        service.prepare(mentions) == service.prepare(mentions.reverse())
    }

    def "a malformed candidate is reported as rejected and never becomes a candidate"() {
        when:
        def result = service.prepare([
            new MentionAddresses('m1', [
                '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb',
                'not-an-address'
            ])
        ])

        then:
        result.valid.empty
        result.rejected*.mentionId == ['m1', 'm1']
        result.rejected*.value == [
            '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb',
            'not-an-address'
        ]
        result.rejected*.network == [Network.BITCOIN, null]
        result.rejected*.reason == [
            RejectionReason.INVALID_ADDRESS,
            RejectionReason.UNKNOWN_FORMAT
        ]
    }
}
