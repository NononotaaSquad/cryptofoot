package squad.nononotaa.backend.service

import java.time.Duration
import java.time.Instant
import squad.nononotaa.backend.analysis.AddressCandidateDetector
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.KindProbabilities
import squad.nononotaa.backend.analysis.PremiseAnswers
import squad.nononotaa.backend.analysis.RawMention
import squad.nononotaa.backend.analysis.ScoringInput
import squad.nononotaa.backend.analysis.SearchService
import squad.nononotaa.backend.analysis.SearchServiceSpec
import squad.nononotaa.backend.analysis.YesNoProbabilities
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.AssessmentMode
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

    /**
     * Scores one item and returns its Attributions as [address, level, premises, rules], sorted by placeholder.
     * A Premise reads as "{mention key}:{placeholder}", so the fixture does not depend on generated Mention ids.
     */
    static List<Map> score(Map item, MentionRepository repository, ConfidenceScoringService scoring) {
        run(item, repository, scoring, AssessmentMode.RULES_ONLY).attributions as List<Map>
    }

    /**
     * Scores one item in [mode] and returns [algorithmVersion, attributions]. With the model on, the item's
     * `cannedAnswers` ("{mention key}:{placeholder}" -> kind probabilities, declaration and warning yes-probability)
     * go into the scoring input; `modelUnavailable: true` means the model gave no answer at all (fallback).
     */
    static Map run(Map item, MentionRepository repository, ConfidenceScoringService scoring, AssessmentMode mode) {
        Map<String, String> addresses = item.addresses as Map<String, String>
        def identifiers = (item.identifiers as List<Map>).collect {
            new CaseIdentifier(IdentifierType.valueOf(it.type as String), fill(it.value as String, addresses))
        }
        def raw = item.fixture ? fixtureMentions(item.fixture as String) : (item.mentions as List<Map>).collect {
            [key: it.key, sourceName: it.sourceName, mention: new RawMention(fill(it.url as String, addresses), AT, fill(it.content as String, addresses), it.author as String)]
        }
        Map<String, String> keyByUrl = raw.collectEntries {
            [(it.mention.url): it.key]
        }
        def sources = raw.groupBy { it.sourceName }.collect { name, mentions ->
            new SearchServiceSpec.FakeSource(name as String, mentions*.mention as List<RawMention>)
        }
        def search = new SearchService(sources, new AddressCandidateDetector(), repository, VALIDATOR, Duration.ofSeconds(10))
        List<FoundMention> mentions = search.search(identifiers, 0).mentions
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

        Map<String, String> keyById = mentions.collectEntries {
            [(it.id): keyByUrl[it.url]]
        }
        Map<String, String> idByKey = keyById.collectEntries { id, key ->
            [(key): id]
        }
        Map<String, PremiseAnswers> answers = mode == AssessmentMode.WITH_MODEL && !item.modelUnavailable ?
                (item.cannedAnswers as Map<String, Map>).collectEntries { String label, Map canned ->
                    def (key, placeholder) = label.split(':', 2) as List<String>
                    assert idByKey.containsKey(key): "item ${item.id}: canned answer ${label} names no Mention"
                    [(idByKey[key] + ':' + canonical(fill(placeholder, addresses))): answersOf(canned)]
                } : [:]
        def result = scoring.score(new ScoringInput(accepted, unclassified, mentions, identifiers, answers, mode))
        def attributions = result.attributions.collect { attribution ->
            Map<String, String> label = attribution.premises.collectEntries {
                [(it.id): "${keyById[it.mentionId]}:${classification[it.address].placeholder}" as String]
            }
            [
                address : classification[attribution.candidate.address].placeholder,
                level   : attribution.confidenceLevel,
                premises: attribution.premises.collect {
                    [id: label[it.id], status: it.status.name(), reason: it.dismissalReason?.name()]
                }.sort { it.id },
                rules   : attribution.appliedRules.collect { rule ->
                    rule.premiseIds.each {
                        assert label.containsKey(it): "item ${item.id}: rule ${rule.rule} names ${it}, not a Premise of its Attribution"
                    }
                    [rule: rule.rule, contribution: rule.contribution, premiseIds: rule.premiseIds.collect {
                            label[it]
                        }.sort()]
                }
            ]
        }.sort { it.address }
        [algorithmVersion: result.algorithmVersion, attributions: attributions]
    }

    /**
     * [actual] in the shape of [expected]: an expected Attribution with a `band` [low, high] takes the band in place
     * of the level when the level is inside it, and its rules are compared by name and Premise ids only.
     */
    static List<Map> banded(List<Map> actual, List<Map> expected) {
        [actual, expected].transpose().collect { Map got, Map wanted ->
            if (!wanted.band) return got
            def (low, high) = wanted.band as List<Integer>
            [
                address : got.address,
                band    : got.level in low..high ? wanted.band : [got.level, got.level],
                premises: got.premises,
                rules   : got.rules.collect {
                    [rule: it.rule, premiseIds: it.premiseIds]
                }
            ]
        } + (actual.size() > expected.size() ? actual.drop(expected.size()) : [])
    }

    private static PremiseAnswers answersOf(Map canned) {
        Map kind = canned.kind as Map
        new PremiseAnswers(
                new KindProbabilities(kind.ownDeclaration as double, kind.saleOrSettlement as double, kind.discussion as double, kind.massComment as double),
                yesNo(canned.declaration as double),
                yesNo(canned.warning as double))
    }

    private static YesNoProbabilities yesNo(double yes) {
        new YesNoProbabilities(yes, 1 - yes)
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

    // the demo fixture is read, not copied, so the golden set and demo-v1 do not drift
    private static List<Map> fixtureMentions(String name) {
        (read("/fixtures/${name}.json").mentions as List<Map>).withIndex().collect { Map it, int index ->
            [key: "m${index + 1}" as String, sourceName: 'synthetic', mention: new RawMention(it.url as String, Instant.parse(it.collectedAt as String), it.content as String, it.author as String)]
        }
    }

    private static Map read(String resource) {
        def stream = GoldenSet.getResourceAsStream(resource)
        assert stream != null: "missing classpath resource ${resource}"
        stream.withCloseable { new ObjectMapper().readValue(it, Map) }
    }
}
