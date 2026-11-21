package squad.nononotaa.backend.repository

import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.model.Mention

class MentionRepositorySpec extends BaseSpec {

    @Autowired
    MentionRepository repository

    def setup() {
        repository.deleteAll()
    }

    def "saves and reads back a mention with all provenance fields"() {
        given:
        def collectedAt = Instant.parse('2026-11-21T10:00:00Z')
        def mention = Mention.create('forum', 'https://forum.example/t/1', collectedAt, 'hello', ['0xabc'])

        when:
        def saved = repository.save(mention)
        def found = repository.findById(saved.id).get()

        then:
        saved.id != null
        found.sourceName == 'forum'
        found.url == 'https://forum.example/t/1'
        found.collectedAt == collectedAt
        found.context == 'hello'
        found.detectedAddresses == ['0xabc']
        found.identity == Mention.computeIdentity('forum', 'https://forum.example/t/1', 'hello')
    }

    def "two mentions with the same identity are both stored"() {
        given:
        def first = Mention.create('forum', 'https://forum.example/t/1', Instant.parse('2026-11-21T10:00:00Z'), 'hello', ['0xabc'])
        def second = Mention.create('forum', 'https://forum.example/t/1', Instant.parse('2026-11-21T11:00:00Z'), 'hello', ['0xabc', 'bc1qxyz'])

        when:
        repository.save(first)
        repository.save(second)
        def found = repository.findByIdentity(Mention.computeIdentity('forum', 'https://forum.example/t/1', 'hello'))

        then:
        found.size() == 2
        found*.detectedAddresses.toSet() == [
            ['0xabc'],
            ['0xabc', 'bc1qxyz']
        ].toSet()
    }

    def "persisted mention maps to FoundMention with its assigned id"() {
        given:
        def collectedAt = Instant.parse('2026-11-21T10:00:00Z')
        def saved = repository.save(Mention.create('forum', 'https://forum.example/t/1', collectedAt, 'hello', ['0xabc']))

        expect:
        saved.toFoundMention() == new FoundMention(saved.id, 'forum', 'https://forum.example/t/1', collectedAt, 'hello', ['0xabc'])
    }
}
