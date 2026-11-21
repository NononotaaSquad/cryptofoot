package squad.nononotaa.backend.analysis

import java.time.Duration
import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.model.Mention
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.MentionRepository
import squad.nononotaa.backend.service.AddressValidator

class SearchServiceSpec extends Specification {

    static class FakeSource implements MentionSource {
        String sourceName
        boolean synthetic = true
        List<RawMention> result = []
        Exception error
        long delayMillis = 0

        FakeSource(String sourceName, List<RawMention> result) {
            this.sourceName = sourceName
            this.result = result
        }

        @Override
        List<RawMention> findMentions(List<CaseIdentifier> identifiers) {
            if (delayMillis> 0) Thread.sleep(delayMillis)
            if (error != null) throw error
            return result
        }
    }

    MentionRepository repository = Mock()
    def detector = new AddressCandidateDetector()
    def validator = new AddressValidator()

    private static Mention withId(Mention m, String id) {
        new Mention(id, m.sourceName, m.url, m.collectedAt, m.context, m.detectedAddresses, m.identity)
    }

    def nickname = new CaseIdentifier(IdentifierType.NICKNAME, 'CryptoNick')
    def rawMention = new RawMention(
    'https://stub.example/p/1',
    Instant.parse('2026-11-21T10:00:00Z'),
    'post by CryptoNick: send BTC to 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa or ETH to 0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed')

    def "checked source with a relevant mention is persisted with snippet, detected addresses and identity"() {
        given:
        def source = new FakeSource('stub-ok', [rawMention])
        def service = new SearchService([source], detector, repository, validator, Duration.ofSeconds(10))
        repository.findByIdentity(_) >> []
        Mention saved = null
        repository.save(_) >> { args ->
            saved = args[0] as Mention; withId(saved, 'm-1')
        }

        when:
        def result = service.search([nickname])

        then:
        result.sources.size() == 1
        result.sources[0].sourceName == 'stub-ok'
        result.sources[0].status == SourceStatus.CHECKED
        result.sources[0].synthetic
        result.sources[0].checkedAt != null

        result.mentions.size() == 1
        def mention = result.mentions[0]
        mention.id == 'm-1'
        mention.sourceName == 'stub-ok'
        mention.url == 'https://stub.example/p/1'
        mention.collectedAt == Instant.parse('2026-11-21T10:00:00Z')
        mention.context.contains('CryptoNick')
        mention.detectedAddresses.containsAll([
            '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
            '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed',
        ])

        saved != null
        saved.id == null
        saved.sourceName == 'stub-ok'
        saved.url == 'https://stub.example/p/1'
        saved.context == mention.context
        saved.detectedAddresses == mention.detectedAddresses
        saved.identity == Mention.computeIdentity('stub-ok', 'https://stub.example/p/1', saved.context)
    }

    def "failing source maps to UNAVAILABLE and search does not throw"() {
        given:
        def failing = new FakeSource('stub-down', [])
        failing.error = new RuntimeException('down')
        def ok = new FakeSource('stub-ok', [rawMention])
        def service = new SearchService([ok, failing], detector, repository, validator, Duration.ofSeconds(10))
        repository.findByIdentity(_) >> []
        repository.save(_) >> { args -> withId(args[0] as Mention, 'm-1') }

        when:
        def result = service.search([nickname])

        then:
        notThrown(Exception)
        result.sources*.sourceName == ['stub-ok', 'stub-down']
        result.sources[0].status == SourceStatus.CHECKED
        result.sources[0].checkedAt != null
        result.sources[1].status == SourceStatus.UNAVAILABLE
        result.sources[1].checkedAt == null
        result.mentions.size() == 1
    }

    def "source exceeding the timeout maps to UNAVAILABLE"() {
        given:
        def slow = new FakeSource('stub-slow', [])
        slow.delayMillis = 5000
        def service = new SearchService([slow], detector, repository, validator, Duration.ofMillis(200))

        when:
        def result = service.search([nickname])

        then:
        notThrown(Exception)
        result.sources.size() == 1
        result.sources[0].sourceName == 'stub-slow'
        result.sources[0].status == SourceStatus.UNAVAILABLE
        result.sources[0].checkedAt == null
        result.mentions.isEmpty()
    }

    def "checked source with zero mentions is CHECKED and distinct from UNAVAILABLE"() {
        given:
        def empty = new FakeSource('stub-empty', [])
        def service = new SearchService([empty], detector, repository, validator, Duration.ofSeconds(10))

        when:
        def result = service.search([nickname])

        then:
        result.sources.size() == 1
        result.sources[0].status == SourceStatus.CHECKED
        result.sources[0].checkedAt != null
        result.mentions.isEmpty()
        0 * repository.save(_)
    }

    def "mention without any identifier is filtered out and not persisted"() {
        given:
        def source = new FakeSource('stub-ok', [
            new RawMention('https://stub.example/p/2', Instant.parse('2026-11-21T10:00:00Z'), 'totally unrelated post about weather')
        ])
        def service = new SearchService([source], detector, repository, validator, Duration.ofSeconds(10))

        when:
        def result = service.search([nickname])

        then:
        result.sources[0].status == SourceStatus.CHECKED
        result.mentions.isEmpty()
        0 * repository.save(_)
    }

    def "nickname match is case-insensitive"() {
        given:
        def source = new FakeSource('stub-ok', [
            new RawMention('https://stub.example/p/3', Instant.parse('2026-11-21T10:00:00Z'), 'CRYPTONICK wrote something')
        ])
        def service = new SearchService([source], detector, repository, validator, Duration.ofSeconds(10))
        repository.findByIdentity(_) >> []
        repository.save(_) >> { args -> withId(args[0] as Mention, 'm-1') }

        when:
        def result = service.search([
            new CaseIdentifier(IdentifierType.NICKNAME, 'cryptonick')
        ])

        then:
        result.mentions.size() == 1
        result.mentions[0].context.contains('CRYPTONICK')
    }

    def "snippet is at most 200 chars and centered around the first match"() {
        given:
        def content = ('x' * 500) + 'CryptoNick' + ('y' * 500)
        def source = new FakeSource('stub-ok', [
            new RawMention('https://stub.example/p/4', Instant.parse('2026-11-21T10:00:00Z'), content)
        ])
        def service = new SearchService([source], detector, repository, validator, Duration.ofSeconds(10))
        repository.findByIdentity(_) >> []
        repository.save(_) >> { args -> withId(args[0] as Mention, 'm-1') }

        when:
        def result = service.search([nickname])

        then:
        result.mentions.size() == 1
        result.mentions[0].context.length() <= 200
        result.mentions[0].context == content.substring(400, 600)
        result.mentions[0].context.contains('CryptoNick')
    }

    def "crypto address identifier requires an exact address match in the content"() {
        given:
        def address = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
        def addressIdentifier = new CaseIdentifier(IdentifierType.NICKNAME, address)
        def exact = new FakeSource('stub-exact', [
            new RawMention('https://stub.example/p/5', Instant.parse('2026-11-21T10:00:00Z'), "send to $address now")
        ])
        def glued = new FakeSource('stub-glued', [
            new RawMention('https://stub.example/p/6', Instant.parse('2026-11-21T10:00:00Z'), "id${address}x")
        ])
        def service = new SearchService([exact, glued], detector, repository, validator, Duration.ofSeconds(10))
        repository.findByIdentity(_) >> []
        repository.save(_) >> { args -> withId(args[0] as Mention, 'm-1') }

        when:
        def result = service.search([addressIdentifier])

        then:
        result.sources.every { it.status == SourceStatus.CHECKED }
        result.mentions.size() == 1
        result.mentions[0].url == 'https://stub.example/p/5'
        result.mentions[0].detectedAddresses == [address]
    }

    def "a wallet stored in canonical form finds a Mention that spells the address in EIP-55 or upper case"() {
        given:
        def wallet = new CaseIdentifier(IdentifierType.WALLET_ADDRESS, '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed')
        def source = new FakeSource('stub-ok', [rawMention])
        def bech32 = new FakeSource('stub-bech32', [
            new RawMention('https://stub.example/p/7', Instant.parse('2026-11-21T10:00:00Z'), 'pay BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4 today')
        ])
        def service = new SearchService([source, bech32], detector, repository, validator, Duration.ofSeconds(10))
        repository.findByIdentity(_) >> []
        repository.save(_) >> { args ->
            withId(args[0] as Mention, "m-${(args[0] as Mention).url}".toString())
        }

        when:
        def result = service.search([
            wallet,
            new CaseIdentifier(IdentifierType.WALLET_ADDRESS, 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4')
        ])

        then:
        result.mentions*.url == [
            'https://stub.example/p/1',
            'https://stub.example/p/7'
        ]
        result.mentions[0].detectedAddresses.contains('0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed')
    }

    def "an invalid address-like identifier still needs the exact text and never matches another address"() {
        given:
        def broken = new CaseIdentifier(IdentifierType.NICKNAME, '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAEd')
        def service = new SearchService([
            new FakeSource('stub-ok', [rawMention])
        ], detector, repository, validator, Duration.ofSeconds(10))

        when:
        def result = service.search([broken])

        then:
        result.mentions.isEmpty()
        0 * repository.save(_)
    }

    def "a full name alone does not make a Mention relevant, a narrowing identifier next to it does"() {
        given:
        def fullName = new CaseIdentifier(IdentifierType.FULL_NAME, 'Jan Kowalski')
        def source = new FakeSource('stub-ok', [
            new RawMention('https://stub.example/p/8', Instant.parse('2026-11-21T10:00:00Z'), 'Jan Kowalski sells BTC 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'),
            new RawMention('https://stub.example/p/9', Instant.parse('2026-11-21T10:00:00Z'), 'Jan Kowalski aka CryptoNick')
        ])
        def service = new SearchService([source], detector, repository, validator, Duration.ofSeconds(10))
        repository.findByIdentity(_) >> []
        repository.save(_) >> { args -> withId(args[0] as Mention, 'm-1') }

        expect:
        service.search([fullName]).mentions.isEmpty()
        service.search([fullName, nickname]).mentions*.url == ['https://stub.example/p/9']
    }

    def "mention already persisted under the same identity is reused, not saved again"() {
        given:
        def identity = Mention.computeIdentity('stub-ok', 'https://stub.example/p/1', rawMention.content)
        def existing = new Mention('m-9', 'stub-ok', 'https://stub.example/p/1', Instant.parse('2026-11-21T10:00:00Z'), rawMention.content, [], identity)
        def source = new FakeSource('stub-ok', [rawMention])
        def service = new SearchService([source], detector, repository, validator, Duration.ofSeconds(10))
        repository.findByIdentity(identity) >> [existing]

        when:
        def result = service.search([nickname])

        then:
        result.mentions.size() == 1
        result.mentions[0].id == 'm-9'
        0 * repository.save(_)
    }

    def "duplicate raw mentions within one search are persisted once"() {
        given:
        def source = new FakeSource('stub-ok', [rawMention, rawMention])
        def service = new SearchService([source], detector, repository, validator, Duration.ofSeconds(10))
        repository.findByIdentity(_) >> []
        repository.save(_) >> { args -> withId(args[0] as Mention, 'm-1') }

        when:
        def result = service.search([nickname])

        then:
        result.mentions.size() == 1
        1 * repository.save(_) >> { args -> withId(args[0] as Mention, 'm-1') }
    }

    def "no registered sources returns an empty result"() {
        given:
        def service = new SearchService([], detector, repository, validator, Duration.ofSeconds(10))

        expect:
        service.search([nickname]) == new SearchResult([], [])
    }
}
