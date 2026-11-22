package squad.nononotaa.backend.analysis

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.AssessmentMode
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.SourceStatus

class AnalysisOrchestratorSpec extends Specification {

    MentionSearch search = Mock()
    CandidatePreparation preparation = Mock()
    ExclusionClassifier exclusions = Mock()
    PremiseAssessment assessment = Mock()
    ConfidenceScoring scoring = Mock()
    AnalysisRecorder recorder = Mock()
    AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(search, preparation, exclusions, assessment, scoring, recorder)
    AssessmentMode mode = AssessmentMode.RULES_ONLY

    def request = new AnalysisRequest('case-1', AnalysisTrigger.MANUAL, [
        new CaseIdentifier(IdentifierType.NICKNAME, 'nick')
    ])
    def mention = new FoundMention('m1', 'forum', 'https://example.test/m1', Instant.parse('2026-11-21T10:00:00Z'), 'ctx', ['0xabc'])
    def okSource = new SourceReport('forum', SourceStatus.CHECKED, true, Instant.parse('2026-11-21T10:00:00Z'))
    def downSource = new SourceReport('market', SourceStatus.UNAVAILABLE, true, null)
    def candidate = new Candidate('ETHEREUM', '0xabc', ['m1'])
    def excludedCandidate = new Candidate('ETHEREUM', '0xdef', ['m1'])
    def attribution = new ScoredAttribution(candidate, 70, [
        new AppliedRule('forum-post', 70)
    ], '', ClassificationStatus.NOT_EXCLUDED)

    AnalysisResult finished

    def setup() {
        recorder.begin(request) >> 'a-1'
        recorder.finish('a-1', _) >> { id, result -> finished = result }
        assessment.getMode() >> { mode }
    }

    static window(String address) {
        new AddressWindow(address, address, "pay to $address", 7, address.length())
    }

    def unclassifiedCandidate = new Candidate('ETHEREUM', '0xfff', ['m1'])
    def windowed = new FoundMention('m1', 'forum', 'https://example.test/m1', Instant.parse('2026-11-21T10:00:00Z'), 'ctx',
    ['0xabc', '0xdef', '0xfff'], 'nick', [
        window('0xabc'),
        window('0xdef'),
        window('0xfff')
    ])
    def answers = new PremiseAnswers(new KindProbabilities(0.8d, 0.1d, 0.05d, 0.05d), new YesNoProbabilities(0.9d, 0.1d),
    new YesNoProbabilities(0.1d, 0.9d), 'req-1')

    def "with the model the port is asked once, only for Premises of accepted and unclassified addresses, and its answers reach scoring"() {
        given:
        mode = AssessmentMode.WITH_MODEL
        search.search(_) >> new SearchResult([windowed], [okSource])
        preparation.prepare(_) >> new PreparedCandidates([
            candidate,
            excludedCandidate,
            unclassifiedCandidate
        ], [
            new RejectedCandidate('m1', 'bad', 'INVALID_ADDRESS')
        ])
        exclusions.classify(_) >> new ExclusionResult([candidate], [
            new ExcludedCandidate(excludedCandidate, ExclusionReason.EXCHANGE, 'own-list')
        ], [unclassifiedCandidate])
        def answered = ['m1:0xabc': answers, 'm1:0xfff': answers]

        when:
        orchestrator.run(request)

        then:
        1 * assessment.assess({ List<PremiseToAssess> premises ->
            premises*.premiseId == ['m1:0xabc', 'm1:0xfff'] &&
            premises*.address == ['0xabc', '0xfff'] &&
            premises*.text == [
                'pay to 0xabc',
                'pay to 0xfff'
            ] &&
            premises[0].facts == PremiseFactsKt.premiseFacts(windowed, window('0xabc'), request.identifiers)
        }, request.identifiers) >> new PremiseAssessments(answered, 'jev-1')
        1 * scoring.score({ ScoringInput input ->
            input.answers == answered && input.mode == AssessmentMode.WITH_MODEL
        }) >>
        new ScoringResult([attribution], 'confidence-3')
        finished.assessmentMode == AssessmentMode.WITH_MODEL
        finished.modelVersion == 'jev-1'
        finished.questionSetVersion == 'premise-questions-1'
        !(ReportLimitation.MODEL_ASSESSMENT_UNAVAILABLE in finished.limitations)
    }

    def "a Premise without an answer adds the Report Limitation once and keeps the status: #variant"() {
        given:
        mode = AssessmentMode.WITH_MODEL
        def other = new Candidate('ETHEREUM', '0xdef', ['m1'])
        search.search(_) >> new SearchResult([windowed], [okSource])
        preparation.prepare(_) >> new PreparedCandidates([candidate, other], [])
        exclusions.classify(_) >> new ExclusionResult([candidate, other], [], [])
        assessment.assess(_, _) >> {
            if (portThrows) throw new IllegalStateException('boom')
            new PremiseAssessments(['m1:0xabc': answers], 'jev-1')
        }
        scoring.score(_) >> new ScoringResult([attribution], 'confidence-3')

        when:
        def run = orchestrator.run(request)

        then:
        run.status == AnalysisStatus.COMPLETED
        finished.status == AnalysisStatus.COMPLETED
        finished.failures.isEmpty()
        finished.limitations == [
            ReportLimitation.MODEL_ASSESSMENT_UNAVAILABLE
        ]
        finished.assessmentMode == AssessmentMode.WITH_MODEL

        where:
        variant              | portThrows
        'one answer missing' | false
        'the port throws'    | true
    }

    def "without the model the port is never asked and no model field or limitation is set"() {
        given:
        search.search(_) >> new SearchResult([windowed], [okSource])
        preparation.prepare(_) >> new PreparedCandidates([candidate], [])
        exclusions.classify(_) >> new ExclusionResult([candidate], [], [])

        when:
        orchestrator.run(request)

        then:
        0 * assessment.assess(_, _)
        1 * scoring.score(new ScoringInput([candidate], [], [windowed], request.identifiers)) >> new ScoringResult([attribution], 'confidence-2')
        finished.limitations.isEmpty()
        finished.assessmentMode == AssessmentMode.RULES_ONLY
        finished.modelVersion == null
        finished.questionSetVersion == null
    }

    def "completes an analysis and hands every stage output to the recorder"() {
        given:
        search.search(request.identifiers) >> new SearchResult([mention], [okSource])
        preparation.prepare([mention]) >> new PreparedCandidates([candidate, excludedCandidate], [
            new RejectedCandidate('m1', 'bad', 'INVALID_ADDRESS')
        ])
        exclusions.classify([candidate, excludedCandidate]) >> new ExclusionResult(
        [candidate], [
            new ExcludedCandidate(excludedCandidate, ExclusionReason.EXCHANGE, 'own-list')
        ], [])

        when:
        def run = orchestrator.run(request)

        then:
        1 * scoring.score(new ScoringInput([candidate], [], [mention], request.identifiers)) >> new ScoringResult([attribution], 'v1')
        run == new AnalysisRun('a-1', AnalysisStatus.COMPLETED)
        finished.status == AnalysisStatus.COMPLETED
        finished.failures.isEmpty()
        finished.attributions == [attribution]
        finished.excluded*.reason == [ExclusionReason.EXCHANGE]
        finished.rejected*.reason == ['INVALID_ADDRESS']
        finished.algorithmVersion == 'v1'
    }

    def "a source that was not checked is no failure, but without any checked source the search fails"() {
        given:
        def skipped = new SourceReport('github', SourceStatus.NOT_CHECKED, false, null)
        search.search(_) >>> [
            new SearchResult([], [okSource, skipped]),
            new SearchResult([], [skipped]),
            new SearchResult([], [skipped, downSource])
        ]

        when:
        def covered = orchestrator.run(request)
        def none = orchestrator.run(request)
        def noneDetail = finished.failures*.detail
        def unavailable = orchestrator.run(request)

        then:
        covered.status == AnalysisStatus.COMPLETED
        none.status == AnalysisStatus.FAILED
        noneDetail == ['NO_SOURCE_CHECKED']
        unavailable.status == AnalysisStatus.FAILED
        finished.failures*.detail == ['ALL_SOURCES_UNAVAILABLE']
    }

    def "no mentions from healthy sources is a completed analysis, not a failure"() {
        given:
        search.search(_) >> new SearchResult([], [okSource])

        when:
        def run = orchestrator.run(request)

        then:
        0 * preparation._
        0 * scoring._
        run.status == AnalysisStatus.COMPLETED
        finished.failures.isEmpty()
        finished.sources == [okSource]
        finished.attributions.isEmpty()
    }

    def "an unavailable source makes the analysis partial and keeps the other findings"() {
        given:
        search.search(_) >> new SearchResult([mention], [okSource, downSource])
        preparation.prepare(_) >> new PreparedCandidates([candidate], [])
        exclusions.classify(_) >> new ExclusionResult([candidate], [], [])
        scoring.score(_) >> new ScoringResult([attribution], 'v1')

        when:
        def run = orchestrator.run(request)

        then:
        run.status == AnalysisStatus.PARTIAL
        finished.failures == [
            new StageFailure(AnalysisStage.SEARCH, 'SOURCE_UNAVAILABLE:market')
        ]
        finished.attributions == [attribution]
    }

    def "an unavailable classification is flagged on the scored Attribution and makes the analysis partial"() {
        given:
        search.search(_) >> new SearchResult([mention], [okSource])
        preparation.prepare(_) >> new PreparedCandidates([candidate, excludedCandidate], [])
        exclusions.classify(_) >> new ExclusionResult([candidate], [], [excludedCandidate])
        def unclassifiedAttribution = new ScoredAttribution(excludedCandidate, 20, [], '', ClassificationStatus.NOT_EXCLUDED)

        when:
        def run = orchestrator.run(request)

        then:
        1 * scoring.score(new ScoringInput([candidate], [excludedCandidate], [mention], request.identifiers)) >> new ScoringResult([
            attribution,
            unclassifiedAttribution
        ], 'v1')
        run.status == AnalysisStatus.PARTIAL
        finished.unclassified == [excludedCandidate]
        finished.failures*.stage == [AnalysisStage.EXCLUSION]
        finished.limitations == [
            ReportLimitation.CLASSIFICATION_UNAVAILABLE,
            ReportLimitation.ANALYSIS_INCOMPLETE
        ]
        finished.attributions*.classificationStatus == [
            ClassificationStatus.NOT_EXCLUDED,
            ClassificationStatus.UNAVAILABLE
        ]
    }

    def "start records the analysis before any module is called and execute finishes it"() {
        when:
        def id = orchestrator.start(request)

        then:
        id == 'a-1'
        0 * search._

        when:
        def run = orchestrator.execute(id, request)

        then:
        1 * search.search(request.identifiers) >> new SearchResult([], [okSource])
        run == new AnalysisRun('a-1', AnalysisStatus.COMPLETED)
        finished.limitations.isEmpty()
    }
    def "all sources unavailable fails the analysis instead of storing zero findings"() {
        given:
        search.search(_) >> new SearchResult([], [downSource])

        when:
        def run = orchestrator.run(request)

        then:
        0 * preparation._
        run.status == AnalysisStatus.FAILED
        finished.failures == [
            new StageFailure(AnalysisStage.SEARCH, 'ALL_SOURCES_UNAVAILABLE')
        ]
    }

    def "a search that checked no source fails the analysis"() {
        given:
        search.search(_) >> new SearchResult([], [])

        expect:
        orchestrator.run(request).status == AnalysisStatus.FAILED
        finished.failures*.detail == ['NO_SOURCE_CHECKED']
    }

    def "a failing module ends the analysis as failed and keeps earlier output"() {
        given:
        search.search(_) >> new SearchResult([mention], [okSource])
        preparation.prepare(_) >> new PreparedCandidates([candidate], [])
        exclusions.classify(_) >> new ExclusionResult([candidate], [], [])
        scoring.score(_) >> { throw new IllegalStateException('boom') }

        when:
        def run = orchestrator.run(request)

        then:
        run.status == AnalysisStatus.FAILED
        finished.failures == [
            new StageFailure(AnalysisStage.SCORING, 'MODULE_ERROR')
        ]
        finished.mentions == [mention]
        finished.attributions.isEmpty()
    }

    def "a failing stage stops later stages"() {
        given:
        search.search(_) >> { throw new IllegalStateException('boom') }

        when:
        def run = orchestrator.run(request)

        then:
        0 * preparation._
        0 * exclusions._
        0 * scoring._
        run.status == AnalysisStatus.FAILED
        finished.failures*.stage == [AnalysisStage.SEARCH]
    }

    def "a failing preparation or exclusion stage fails the analysis"() {
        given:
        search.search(_) >> new SearchResult([mention], [okSource])
        preparation.prepare(_) >> {
            if (failAt == AnalysisStage.CANDIDATE_PREPARATION) throw new IllegalStateException(); new PreparedCandidates([candidate], [])
        }
        exclusions.classify(_) >> { throw new IllegalStateException() }

        when:
        def run = orchestrator.run(request)

        then:
        0 * scoring._
        run.status == AnalysisStatus.FAILED
        finished.failures*.stage == [failAt]

        where:
        failAt << [
            AnalysisStage.CANDIDATE_PREPARATION,
            AnalysisStage.EXCLUSION
        ]
    }
}
