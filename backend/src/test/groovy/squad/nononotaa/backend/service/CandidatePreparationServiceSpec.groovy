package squad.nononotaa.backend.service

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.RejectedCandidate

class CandidatePreparationServiceSpec extends Specification {

    static final Instant AT = Instant.parse('2026-11-21T10:00:00Z')
    static final String ETH_CHECKSUMMED = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'
    static final String ETH = '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
    static final String BTC = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
    static final String BECH32_UPPER = 'BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4'

    def service = new CandidatePreparationService(new AddressCandidateService(new AddressValidator()))

    private static FoundMention mention(String id, List<String> addresses) {
        new FoundMention(id, 'forum', "https://forum.example/${id}", AT, 'ctx', addresses)
    }

    def "the addresses detected in Mentions become canonical candidates grouped by address with their Mentions"() {
        when:
        def prepared = service.prepare([
            mention('m2', [ETH_CHECKSUMMED, BTC]),
            mention('m1', [ETH, BECH32_UPPER]),
        ])

        then:
        prepared.valid == [
            new Candidate('BTC', BTC, ['m2']),
            new Candidate('BTC', BECH32_UPPER.toLowerCase(), ['m1']),
            new Candidate('ETH', ETH, ['m1', 'm2']),
        ]
        prepared.rejected.isEmpty()
    }

    def "a detected string that is not a valid address is rejected with its Mention and failure and never becomes a candidate"() {
        when:
        def prepared = service.prepare([
            mention('m1', [
                '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAEd',
                '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb',
                BTC
            ])
        ])

        then:
        prepared.valid == [
            new Candidate('BTC', BTC, ['m1'])
        ]
        prepared.rejected == [
            new RejectedCandidate('m1', '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAEd', 'CHECKSUM_MISMATCH'),
            new RejectedCandidate('m1', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb', 'CHECKSUM_MISMATCH'),
        ]
    }

    def "the order of Mentions does not change the prepared input of the Confidence Level"() {
        given:
        def first = mention('m1', [ETH, BTC])
        def second = mention('m2', [ETH_CHECKSUMMED])

        expect:
        service.prepare([first, second]) == service.prepare([second, first])
    }

    def "Mentions without detected addresses give no candidates"() {
        expect:
        service.prepare([mention('m1', [])]).valid.isEmpty()
        service.prepare([]).rejected.isEmpty()
    }
}
