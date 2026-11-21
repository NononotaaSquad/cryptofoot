package squad.nononotaa.backend.model

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.analysis.FoundMention

class MentionSpec extends Specification {

    def "identity is the sha256 of source name, url and context"() {
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

    def "null context is treated as empty string"() {
        expect:
        Mention.computeIdentity('forum', 'https://forum.example/t/1', null) ==
                Mention.computeIdentity('forum', 'https://forum.example/t/1', '')
    }

    def "new mention is unpersisted and carries identity and detected addresses"() {
        when:
        def mention = Mention.create('forum', 'https://forum.example/t/1', Instant.parse('2026-11-21T10:00:00Z'), 'hello', ['0xabc'])

        then:
        mention.id == null
        mention.identity == Mention.computeIdentity('forum', 'https://forum.example/t/1', 'hello')
        mention.detectedAddresses == ['0xabc']
        mention.collectedAt == Instant.parse('2026-11-21T10:00:00Z')
    }

    def "persisted mention maps to FoundMention"() {
        given:
        def collectedAt = Instant.parse('2026-11-21T10:00:00Z')
        def mention = new Mention('m1', 'forum', 'https://forum.example/t/1', collectedAt, 'hello', ['0xabc'], 'hash')

        expect:
        mention.toFoundMention() == new FoundMention('m1', 'forum', 'https://forum.example/t/1', collectedAt, 'hello', ['0xabc'])
    }
}
