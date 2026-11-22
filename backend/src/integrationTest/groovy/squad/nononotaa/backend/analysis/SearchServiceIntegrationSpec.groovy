package squad.nononotaa.backend.analysis

import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.model.Mention
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.MentionRepository
import squad.nononotaa.testsupport.StubMentionSources

@Import(StubMentionSources)
class SearchServiceIntegrationSpec extends BaseSpec {

    @Autowired
    MentionSearch searchService

    @Autowired
    MentionRepository repository

    @DynamicPropertySource
    static void searchProperties(DynamicPropertyRegistry registry) {
        registry.add('sources.timeout') { '300ms' }
        registry.add('sources.synthetic.enabled') { 'false' }
    }

    def setup() {
        repository.deleteAll()
    }

    def "search persists relevant mentions with full provenance and reports per-source outcomes"() {
        when:
        def result = searchService.search([
            new CaseIdentifier(IdentifierType.NICKNAME, 'CryptoNick')
        ], 0)

        then:
        result.sources.size() == 4
        def byName = result.sources.collectEntries { [(it.sourceName): it] }
        byName['stub-ok'].status == SourceStatus.CHECKED
        byName['stub-ok'].synthetic
        byName['stub-ok'].checkedAt != null
        byName['stub-empty'].status == SourceStatus.CHECKED
        byName['stub-empty'].checkedAt != null
        byName['stub-failing'].status == SourceStatus.UNAVAILABLE
        byName['stub-failing'].checkedAt == null
        byName['stub-slow'].status == SourceStatus.UNAVAILABLE
        byName['stub-slow'].checkedAt == null

        result.mentions.size() == 1
        def mention = result.mentions[0]
        mention.id != null
        mention.sourceName == 'stub-ok'
        mention.url == 'https://stub-ok.example/post/1'
        mention.collectedAt == Instant.parse('2026-11-21T10:00:00Z')
        mention.context.contains('CryptoNick')
        mention.context.length() <= 200
        mention.detectedAddresses.containsAll([
            '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
            '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed',
        ])

        def persisted = repository.findAll()
        persisted.size() == 1
        def doc = persisted[0]
        doc.id == mention.id
        doc.sourceName == 'stub-ok'
        doc.url == mention.url
        doc.collectedAt == Instant.parse('2026-11-21T10:00:00Z')
        doc.context == mention.context
        doc.detectedAddresses == mention.detectedAddresses
        doc.identity == Mention.computeIdentity('stub-ok', mention.url, mention.context)
    }

    def "repeated search reuses the persisted mention instead of duplicating it"() {
        when:
        searchService.search([
            new CaseIdentifier(IdentifierType.NICKNAME, 'CryptoNick')
        ], 0)
        def result = searchService.search([
            new CaseIdentifier(IdentifierType.NICKNAME, 'CryptoNick')
        ], 0)

        then:
        result.mentions.size() == 1
        repository.findAll().size() == 1
    }
}
