package squad.nononotaa.backend.repository

import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AddressWindow
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
        def mention = Mention.create('forum', 'https://forum.example/t/1', collectedAt, 'hello', 'hello', ['0xabc'])

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
        def first = Mention.create('forum', 'https://forum.example/t/1', Instant.parse('2026-11-21T10:00:00Z'), 'hello', 'hello', ['0xabc'])
        def second = Mention.create('forum', 'https://forum.example/t/1', Instant.parse('2026-11-21T11:00:00Z'), 'hello', 'hello', ['0xabc', 'bc1qxyz'])

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
        def saved = repository.save(Mention.create('forum', 'https://forum.example/t/1', collectedAt, 'hello', 'hello', ['0xabc']))

        expect:
        saved.toFoundMention() == new FoundMention(saved.id, 'forum', 'https://forum.example/t/1', collectedAt, 'hello', ['0xabc'])
    }

    def "address windows and the author survive a save and a read"() {
        given:
        def windows = [
            new AddressWindow('0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed', '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed', 'pay 0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed now', 4, 42),
            new AddressWindow('bc1qxyz', null, 'or bc1qxyz', 3, 7),
        ]
        def mention = Mention.create('forum', 'https://forum.example/t/1', Instant.parse('2026-11-21T10:00:00Z'), 'full page', 'excerpt',
                windows*.rawAddress, 'nick', windows)

        when:
        def found = repository.findById(repository.save(mention).id).get()

        then:
        found.addressWindows == windows
        found.author == 'nick'
        found.toFoundMention().addressWindows == windows
    }
}
