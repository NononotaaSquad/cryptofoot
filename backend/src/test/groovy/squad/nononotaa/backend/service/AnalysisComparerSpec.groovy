package squad.nononotaa.backend.service

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.analysis.AnalysisResult
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.ExcludedCandidate
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.SourceReport
import squad.nononotaa.backend.model.AnalysisRecord
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.AssessmentMode
import squad.nononotaa.backend.model.AttributionChangeType as Type
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ComparabilityLimitation as Limit
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.SourceStatus

class AnalysisComparerSpec extends Specification {

    def comparer = new AnalysisComparer()
    static final T0 = Instant.parse('2026-11-21T10:00:00Z')
    static final ADDR = '0xaaa'

    static attribution(String network, String address, int level, List<String> mentionIds = ['m1']) {
        new ScoredAttribution(new Candidate(network, address, mentionIds), level, [], '', ClassificationStatus.NOT_EXCLUDED)
    }

    static excluded(String network, String address, ExclusionReason reason, List<String> mentionIds = ['m1']) {
        new ExcludedCandidate(new Candidate(network, address, mentionIds), reason, 'own-list')
    }

    static record(Map args = [:]) {
        def sources = args.get('sources', [
            new SourceReport('forum', SourceStatus.CHECKED, true, T0)
        ])
        def result = new AnalysisResult(args.get('status', AnalysisStatus.COMPLETED), [], [], sources,
        args.get('mentions', [
            new FoundMention('m1', 'forum', 'https://example.test/m1', T0, null, [ADDR])
        ]),
        [], args.get('excluded', []), [], args.get('attributions', []), args.get('version', 'v1'),
        args.get('mode', AssessmentMode.RULES_ONLY), args.get('modelVersion', null), null)
        new AnalysisRecord('id', 0L, 'case-1', AnalysisTrigger.MANUAL, result.status, T0, T0,
                args.get('identifiers', [
                    new CaseIdentifier(IdentifierType.NICKNAME, 'nick')
                ]),
                args.get('networks', [Network.BTC, Network.ETH]), result)
    }

    def "reports a new Attribution"() {
        when:
        def changes = comparer.changes(record(), record(attributions: [attribution('ETH', ADDR, 70)]))

        then:
        changes*.change == [Type.NEW]
        changes[0].laterConfidenceLevel == 70
        changes[0].earlierConfidenceLevel == null
        changes[0].absenceVerifiable == null
        changes[0].laterMentionIds == ['m1']
        changes[0].earlierMentionIds == []
    }

    def "reports a changed Confidence Level with the evidence of both sides"() {
        when:
        def changes = comparer.changes(record(attributions: [
            attribution('ETH', ADDR, 50, ['m1'])
        ]),
        record(attributions: [
            attribution('ETH', ADDR, 65, ['m1', 'm2'])
        ]))

        then:
        changes*.change == [Type.CONFIDENCE_LEVEL_CHANGED]
        changes[0].earlierConfidenceLevel == 50
        changes[0].laterConfidenceLevel == 65
        changes[0].earlierMentionIds == ['m1']
        changes[0].laterMentionIds == ['m1', 'm2']
    }

    def "a changed Confidence Level is flagged as a changed model assessment only with the same Mentions and the model on one side"() {
        when:
        def changes = comparer.changes(
                record(mode: earlierMode, attributions: [
                    attribution('ETH', ADDR, 50, ['m1', 'm2'])
                ]),
                record(mode: laterMode, attributions: [
                    attribution('ETH', ADDR, 65, laterMentions)
                ]))

        then:
        changes*.change == [Type.CONFIDENCE_LEVEL_CHANGED]
        changes[0].modelAssessmentChanged == flagged

        where:
        earlierMode                | laterMode                  | laterMentions        | flagged
        AssessmentMode.RULES_ONLY  | AssessmentMode.WITH_MODEL  | ['m2', 'm1']         | true
        AssessmentMode.WITH_MODEL  | AssessmentMode.WITH_MODEL  | ['m1', 'm2', 'm3']   | false
        AssessmentMode.RULES_ONLY  | AssessmentMode.RULES_ONLY  | ['m1', 'm2']         | false
    }

    def "a new Attribution is never flagged as a changed model assessment"() {
        expect:
        !comparer.changes(record(mode: AssessmentMode.WITH_MODEL),
        record(mode: AssessmentMode.WITH_MODEL, attributions: [attribution('ETH', ADDR, 70)]))[0].modelAssessmentChanged
    }

    def "an extra copy of a Mention with the same Confidence Level is not a change"() {
        expect:
        comparer.changes(record(attributions: [
            attribution('ETH', ADDR, 50, ['m1'])
        ]),
        record(attributions: [
            attribution('ETH', ADDR, 50, ['m1', 'm1-copy'])
        ])).isEmpty()
    }

    def "reports an Attribution not observed later as verifiable only when the supporting source was checked again"() {
        when:
        def changes = comparer.changes(record(attributions: [attribution('ETH', ADDR, 70)]), record(
                sources: laterSources, status: laterStatus, networks: laterNetworks))

        then:
        changes*.change == [Type.NOT_OBSERVED]
        changes[0].earlierConfidenceLevel == 70
        changes[0].laterConfidenceLevel == null
        changes[0].absenceVerifiable == verifiable

        where:
        laterSources                                                    | laterStatus                | laterNetworks             | verifiable
        [
            new SourceReport('forum', SourceStatus.CHECKED, true, T0)
        ]     | AnalysisStatus.COMPLETED   | [Network.BTC, Network.ETH] | true
        [
            new SourceReport('forum', SourceStatus.UNAVAILABLE, true, T0)
        ] | AnalysisStatus.PARTIAL     | [Network.BTC, Network.ETH] | false
        [
            new SourceReport('forum', SourceStatus.CHECKED, true, T0)
        ]     | AnalysisStatus.PARTIAL     | [Network.BTC, Network.ETH] | false
        [
            new SourceReport('other', SourceStatus.CHECKED, true, T0)
        ]     | AnalysisStatus.COMPLETED   | [Network.BTC, Network.ETH] | false
        [
            new SourceReport('forum', SourceStatus.CHECKED, true, T0)
        ]     | AnalysisStatus.COMPLETED   | [Network.BTC]              | false
        []                                                              | AnalysisStatus.FAILED      | [Network.BTC, Network.ETH] | false
    }

    def "reports a changed exclusion classification in both directions and for a changed reason"() {
        when:
        def changes = comparer.changes(record(attributions: earlierAttr, excluded: earlierExcl),
        record(attributions: laterAttr, excluded: laterExcl))

        then:
        changes*.change == [Type.EXCLUSION_CHANGED]
        changes[0].earlierExclusionReason == earlierReason
        changes[0].laterExclusionReason == laterReason

        where:
        earlierAttr                          | earlierExcl                                   | laterAttr                            | laterExcl                                     | earlierReason             | laterReason
        [attribution('ETH', ADDR, 60)]       | []                                            | []                                   | [
            excluded('ETH', ADDR, ExclusionReason.MIXER)
        ] | null                      | ExclusionReason.MIXER
        []                                   | [
            excluded('ETH', ADDR, ExclusionReason.MIXER)
        ] | [attribution('ETH', ADDR, 60)]       | []                                            | ExclusionReason.MIXER     | null
        []                                   | [
            excluded('ETH', ADDR, ExclusionReason.MIXER)
        ] | []                                   | [
            excluded('ETH', ADDR, ExclusionReason.EXCHANGE)
        ] | ExclusionReason.MIXER | ExclusionReason.EXCHANGE
    }

    def "does not report an unchanged exclusion or an exclusion seen on one side only"() {
        expect:
        comparer.changes(record(excluded: [
            excluded('ETH', ADDR, ExclusionReason.MIXER)
        ]),
        record(excluded: [
            excluded('ETH', ADDR, ExclusionReason.MIXER)
        ])).isEmpty()
        comparer.changes(record(), record(excluded: [
            excluded('ETH', ADDR, ExclusionReason.MIXER)
        ])).isEmpty()
        comparer.changes(record(excluded: [
            excluded('ETH', ADDR, ExclusionReason.MIXER)
        ]), record()).isEmpty()
    }

    def "matches by network and address: the same text on another network is a different address"() {
        when:
        def changes = comparer.changes(record(attributions: [attribution('ETH', ADDR, 70)]),
        record(attributions: [attribution('BTC', ADDR, 70)]))

        then:
        changes.collect {
            [it.network, it.change]
        } == [
            [Network.BTC, Type.NEW],
            [
                Network.ETH,
                Type.NOT_OBSERVED
            ]
        ]
    }

    def "an unchanged Attribution gives no change"() {
        expect:
        comparer.changes(record(attributions: [attribution('ETH', ADDR, 70)]), record(attributions: [attribution('ETH', ADDR, 70)])).isEmpty()
    }

    def "lists the reasons why two analyses are not comparable"() {
        expect:
        comparer.limitations(record(), record()) == []
        comparer.limitations(record(), record(identifiers: [
            new CaseIdentifier(IdentifierType.NICKNAME, 'other')
        ])) == [Limit.IDENTIFIERS_DIFFER]
        comparer.limitations(record(), record(identifiers: [
            new CaseIdentifier(IdentifierType.NICKNAME, 'nick'),
            new CaseIdentifier(IdentifierType.WALLET_ADDRESS, '0xabc')
        ])) == [Limit.IDENTIFIERS_DIFFER]
        comparer.limitations(record(), record(sources: [
            new SourceReport('forum', SourceStatus.UNAVAILABLE, true, null)
        ])) == [Limit.SOURCE_COVERAGE_DIFFERS]
        comparer.limitations(record(), record(sources: [
            new SourceReport('forum', SourceStatus.CHECKED, true, T0),
            new SourceReport('news', SourceStatus.CHECKED, false, T0)
        ])) == [Limit.SOURCE_COVERAGE_DIFFERS]
        comparer.limitations(record(), record(networks: [Network.BTC])) == [Limit.NETWORKS_DIFFER]
        comparer.limitations(record(), record(version: 'v2')) == [
            Limit.ALGORITHM_VERSION_DIFFERS
        ]
        comparer.limitations(record(), record(status: AnalysisStatus.PARTIAL)) == [Limit.PARTIAL_ANALYSIS]
        comparer.limitations(record(mode: AssessmentMode.WITH_MODEL, modelVersion: 'jev-1'),
        record(mode: AssessmentMode.WITH_MODEL, modelVersion: 'jev-2')) == [Limit.MODEL_VERSION_DIFFERS]
        comparer.limitations(record(), record(mode: AssessmentMode.WITH_MODEL, modelVersion: 'jev-1')) == [Limit.MODEL_VERSION_DIFFERS]
        comparer.limitations(record(mode: AssessmentMode.WITH_MODEL, modelVersion: 'jev-1'),
        record(mode: AssessmentMode.WITH_MODEL, modelVersion: 'jev-1')) == []
        comparer.limitations(record(status: AnalysisStatus.FAILED), record()) == [Limit.PARTIAL_ANALYSIS]
    }
}
