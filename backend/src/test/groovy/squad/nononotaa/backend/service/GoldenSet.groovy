package squad.nononotaa.backend.service

import java.time.Duration
import java.time.Instant
import squad.nononotaa.backend.analysis.AddressCandidateDetector
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.RawMention
import squad.nononotaa.backend.analysis.ScoringInput
import squad.nononotaa.backend.analysis.SearchService
import squad.nononotaa.backend.analysis.SearchServiceSpec
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.Mention
import squad.nononotaa.backend.repository.MentionRepository
import tools.jackson.databind.ObjectMapper

/**
 * Golden set harness (P26): builds the scoring input the way the app does, through the real SearchService
 * and CandidatePreparationService, and takes the exclusion classification from the fixture.
 */
class GoldenSet {

    static final String RESOURCE = '/golden-set/golden-set-v1.json'
    static final Instant AT = Instant.parse('2026-11-21T10:00:00Z')
    static final AddressValidator VALIDATOR = new AddressValidator()

    static List<Map> items() {
        read(RESOURCE).items as List<Map>
    }

    /** [item id, placeholder, address] for every address the fixture names, placeholders resolved. */
    static List<List<String>> addresses() {
        items().collectMany { Map item ->
            Map<String, String> addresses = item.addresses as Map<String, String>
            def keys = (addresses.keySet() + (item.classification as Map).keySet()) as LinkedHashSet<String>
            keys.collect { String key ->
                [
                    item.id as String,
                    key,
                    fill(key, addresses)
                ]
            }
        }
    }

    static Mention withId(Mention m, String id) {
        new Mention(id, m.sourceName, m.url, m.collectedAt, m.context, m.detectedAddresses, m.identity, m.author, m.addressWindows)
    }

    /** Scores one item and returns its Attributions as [address: placeholder, level, rules], sorted by placeholder. */
    static List<Map> score(Map item, MentionRepository repository, ConfidenceScoringService scoring) {
        Map<String, String> addresses = item.addresses as Map<String, String>
        def identifiers = (item.identifiers as List<Map>).collect {
            new CaseIdentifier(IdentifierType.valueOf(it.type as String), fill(it.value as String, addresses))
        }
        def raw = item.fixture ? fixtureMentions(item.fixture as String) : (item.mentions as List<Map>).collect {
            [sourceName: it.sourceName, mention: new RawMention(fill(it.url as String, addresses), AT, fill(it.content as String, addresses), it.author as String)]
        }
        def sources = raw.groupBy { it.sourceName }.collect { name, mentions ->
            new SearchServiceSpec.FakeSource(name as String, mentions*.mention as List<RawMention>)
        }
        def search = new SearchService(sources, new AddressCandidateDetector(), repository, VALIDATOR, Duration.ofSeconds(10))
        List<FoundMention> mentions = search.search(identifiers).mentions
        List<Candidate> valid = new CandidatePreparationService(new AddressCandidateService(VALIDATOR)).prepare(mentions).valid

        Map<String, Map> classification = (item.classification as Map<String, String>).collectEntries { key, status ->
            [(canonical(fill(key, addresses))): [placeholder: key, status: status]]
        }
        valid.each {
            assert classification.containsKey(it.address): "item ${item.id}: valid candidate ${it.address} has no classification"
        }
        def accepted = valid.findAll {
            classification[it.address].status == 'ACCEPTED'
        }
        def unclassified = valid.findAll {
            classification[it.address].status == 'UNCLASSIFIED'
        }

        scoring.score(new ScoringInput(accepted, unclassified, mentions, identifiers)).attributions.collect {
            [address: classification[it.candidate.address].placeholder, level: it.confidenceLevel, rules: it.appliedRules*.rule]
        }.sort { it.address }
    }

    static String fill(String text, Map<String, String> addresses) {
        if (text == null) return null
        addresses.keySet().sort {
            -it.length()
        }.inject(text) { String t, String key ->
            t.replace(key, addresses[key])
        }
    }

    private static String canonical(String address) {
        def validation = VALIDATOR.validate(address, null)
        assert validation instanceof AddressValidation.Valid: "fixture address ${address} is not valid"
        (validation as AddressValidation.Valid).canonicalAddress.address
    }

    // the demo fixture is read, not copied, so the golden set and demo-v1 do not drift; SyntheticMentionSource has no author
    private static List<Map> fixtureMentions(String name) {
        (read("/fixtures/${name}.json").mentions as List<Map>).collect {
            [sourceName: 'synthetic', mention: new RawMention(it.url as String, Instant.parse(it.collectedAt as String), it.content as String)]
        }
    }

    private static Map read(String resource) {
        def stream = GoldenSet.getResourceAsStream(resource)
        assert stream != null: "missing classpath resource ${resource}"
        stream.withCloseable { new ObjectMapper().readValue(it, Map) }
    }
}
