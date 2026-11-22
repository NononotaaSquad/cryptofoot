package squad.nononotaa.backend.analysis

import org.springframework.beans.factory.annotation.Autowired
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.model.Mention
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.MentionRepository
import tools.jackson.databind.ObjectMapper

class SyntheticSourceIntegrationSpec extends BaseSpec {

    @Autowired
    MentionSearch searchService

    @Autowired
    MentionRepository repository

    def setup() {
        repository.deleteAll()
    }

    def "demo-v1 search returns the fixture mentions with candidates and exactly one unavailable source"() {
        when:
        def result = searchService.search([
            new CaseIdentifier(IdentifierType.NICKNAME, 'zenek_fx')
        ])

        then:
        result.sources.size() == 2
        def byName = result.sources.collectEntries { [(it.sourceName): it] }
        byName['synthetic'].status == SourceStatus.CHECKED
        byName['synthetic'].synthetic
        byName['synthetic'].checkedAt != null
        byName['synthetic-broken'].status == SourceStatus.UNAVAILABLE
        byName['synthetic-broken'].synthetic
        result.sources.count { it.status == SourceStatus.UNAVAILABLE } == 1

        result.mentions.size() == 3
        def byUrl = result.mentions.collectEntries { [(it.url): it] }
        byUrl['https://forum.example/t/porady-p2p-od-zenka/123'].detectedAddresses == [
            '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
            '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed',
        ]
        byUrl['https://x.example/zenek_fx/status/555'].detectedAddresses == [
            '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
            '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed',
        ]
        byUrl['https://p2p.example/ad/987'].detectedAddresses == [
            'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4',
            '0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae',
        ]
        result.mentions.every { it.sourceName == 'synthetic' && it.id != null }
        !result.mentions.any { m ->
            m.detectedAddresses.contains('1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfN0')
        }

        def persisted = repository.findAll()
        persisted.size() == 3
        persisted.every { it.sourceName == 'synthetic' }
        def contentByUrl = fixtureContentByUrl()
        persisted.every {
            it.identity == Mention.computeIdentity(it.sourceName, it.url, contentByUrl[it.url])
        }
        persisted.every { m ->
            m.addressWindows*.rawAddress == m.detectedAddresses &&
                    m.addressWindows.every {
                        it.address != null && it.text.substring(it.addressStart, it.addressStart + it.addressLength) == it.rawAddress
                    }
        }
    }

    def "searching by an identifier present in only one mention keeps only that mention"() {
        when:
        def result = searchService.search([
            new CaseIdentifier(IdentifierType.EMAIL, 'zenon.fikcyjny@example.com')
        ])

        then:
        result.mentions.size() == 1
        result.mentions[0].url == 'https://forum.example/t/porady-p2p-od-zenka/123'
        repository.findAll().size() == 1
    }

    def "a repeated demo-v1 search reuses the stored Mentions, also a page longer than the excerpt"() {
        given:
        def nickname = [
            new CaseIdentifier(IdentifierType.NICKNAME, 'zenek_fx')
        ]
        def first = searchService.search(nickname)

        when:
        def second = searchService.search(nickname)

        then:
        repository.findAll().size() == 3
        second.mentions*.id.toSet() == first.mentions*.id.toSet()
    }

    private static Map<String, String> fixtureContentByUrl() {
        def fixture = SyntheticSourceIntegrationSpec.getResourceAsStream('/fixtures/demo-v1.json').withCloseable {
            new ObjectMapper().readValue(it, Map)
        }
        (fixture.mentions as List<Map>).collectEntries {
            [(it.url): it.content]
        }
    }
}
