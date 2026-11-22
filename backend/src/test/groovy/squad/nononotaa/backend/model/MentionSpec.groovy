package squad.nononotaa.backend.model

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.analysis.AddressWindow
import squad.nononotaa.backend.analysis.FoundMention

class MentionSpec extends Specification {

    def "identity is the sha256 of source name, url and full content"() {
        expect:
        Mention.computeIdentity('forum', 'https://forum.example/t/1', 'hello') ==
                '65864a6a3fbb7a84ed4663212b275dd99038040947e30d67984edaf8133f72fd'
    }

    def "identity is deterministic and changes with any input"() {
        expect:
        Mention.computeIdentity('forum', 'https://forum.example/t/1', 'a') ==
                Mention.computeIdentity('forum', 'https://forum.example/t/1', 'a')
        Mention.computeIdentity('forum', 'https://forum.example/t/1', 'a') !=
                Mention.computeIdentity('forum', 'https://forum.example/t/1', 'b')
        Mention.computeIdentity('forum', 'https://forum.example/t/1', 'a') !=
                Mention.computeIdentity('other', 'https://forum.example/t/1', 'a')
        Mention.computeIdentity('forum', 'https://forum.example/t/1', 'a') !=
                Mention.computeIdentity('forum', 'https://forum.example/t/2', 'a')
    }

    def "new mention is unpersisted and carries identity and detected addresses"() {
        when:
        def mention = Mention.create('forum', 'https://forum.example/t/1', Instant.parse('2026-11-21T10:00:00Z'), 'hello', 'hello', ['0xabc'])

        then:
        mention.id == null
        mention.identity == Mention.computeIdentity('forum', 'https://forum.example/t/1', 'hello')
        mention.detectedAddresses == ['0xabc']
        mention.collectedAt == Instant.parse('2026-11-21T10:00:00Z')
    }

    def "new mention hashes the full content for its identity and keeps only the excerpt as context"() {
        given:
        def window = new AddressWindow('0xabc', null, 'pay 0xabc', 4, 5)

        when:
        def mention = Mention.create('forum', 'https://forum.example/t/1', Instant.parse('2026-11-21T10:00:00Z'), 'long page: pay 0xabc', 'pay 0xabc', ['0xabc'], 'nick', [window])

        then:
        mention.identity == Mention.computeIdentity('forum', 'https://forum.example/t/1', 'long page: pay 0xabc')
        mention.identity != Mention.computeIdentity('forum', 'https://forum.example/t/1', 'pay 0xabc')
        mention.context == 'pay 0xabc'
        mention.author == 'nick'
        mention.addressWindows == [window]
    }

    def "persisted mention maps to FoundMention"() {
        given:
        def collectedAt = Instant.parse('2026-11-21T10:00:00Z')
        def mention = new Mention('m1', 'forum', 'https://forum.example/t/1', collectedAt, 'hello', ['0xabc'], 'hash')

        expect:
        mention.toFoundMention() == new FoundMention('m1', 'forum', 'https://forum.example/t/1', collectedAt, 'hello', ['0xabc'])
    }

    def "persisted mention passes its author and address windows to FoundMention"() {
        given:
        def collectedAt = Instant.parse('2026-11-21T10:00:00Z')
        def window = new AddressWindow('0xabc', null, 'pay 0xabc', 4, 5)
        def mention = new Mention('m1', 'forum', 'https://forum.example/t/1', collectedAt, 'hello', ['0xabc'], 'hash', 'nick', [window])

        expect:
        mention.toFoundMention() == new FoundMention('m1', 'forum', 'https://forum.example/t/1', collectedAt, 'hello', ['0xabc'], 'nick', [window])
    }
}
