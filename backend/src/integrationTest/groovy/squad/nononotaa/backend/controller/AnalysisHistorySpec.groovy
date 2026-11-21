package squad.nononotaa.backend.controller

import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.AnalysisResult
import squad.nononotaa.backend.analysis.AnalysisStage
import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.ExcludedCandidate
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.SourceReport
import squad.nononotaa.backend.analysis.StageFailure
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.AnalysisRepository

class AnalysisHistorySpec extends BaseSpec {

    @Autowired
    AnalysisRecorder recorder

    @Autowired
    AnalysisRepository repository

    String caseId = "case-${UUID.randomUUID()}"
    def checkedAt = Instant.parse('2026-11-21T10:00:00Z')
    def candidate = new Candidate('ETH', '0xabc', ['m1'])
    def mention = new FoundMention('m1', 'forum', 'https://example.test/m1', checkedAt, 'ctx', ['0xabc'])

    private AnalysisRequest request(AnalysisTrigger trigger = AnalysisTrigger.MANUAL, String nickname = 'nick') {
        new AnalysisRequest(caseId, trigger, [
            new CaseIdentifier(IdentifierType.NICKNAME, nickname),
            new CaseIdentifier(IdentifierType.EMAIL, 'a@b.pl')
        ])
    }

    private AnalysisResult completed(String version = 'v1') {
        new AnalysisResult(AnalysisStatus.COMPLETED, [], [], [
            new SourceReport('forum', SourceStatus.CHECKED, true, checkedAt)
        ], [mention], [],
        [
            new ExcludedCandidate(new Candidate('ETH', '0xdef', ['m1']), ExclusionReason.EXCHANGE, 'own-list')
        ], [],
        [
            new ScoredAttribution(candidate, 70, [
                new AppliedRule('forum-post', 70)
            ], '', ClassificationStatus.NOT_EXCLUDED)
        ], version)
    }

    private AnalysisResult failed() {
        new AnalysisResult(AnalysisStatus.FAILED, [
            new StageFailure(AnalysisStage.SEARCH, 'ALL_SOURCES_UNAVAILABLE')
        ],
        [
            ReportLimitation.SOURCE_UNAVAILABLE,
            ReportLimitation.ANALYSIS_INCOMPLETE
        ], [
            new SourceReport('forum', SourceStatus.UNAVAILABLE, true, null)
        ],
        [], [], [], [], [], null)
    }

    private String body(String uri) {
        client.get().uri(uri).exchange().expectStatus().isOk().expectBody(String).returnResult().responseBody
    }

    def "a started analysis is readable as running with the identifiers used"() {
        when:
        def id = recorder.begin(request(AnalysisTrigger.SCHEDULED))

        then:
        client.get().uri("/api/1/analyses/${id}").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.id').isEqualTo(id)
                .jsonPath('$.caseId').isEqualTo(caseId)
                .jsonPath('$.status').isEqualTo('RUNNING')
                .jsonPath('$.trigger').isEqualTo('SCHEDULED')
                .jsonPath('$.startedAt').exists()
                .jsonPath('$.finishedAt').isEmpty()
                .jsonPath('$.identifiers.nickname').isEqualTo('nick')
                .jsonPath('$.identifiers.email').isEqualTo('a@b.pl')
                .jsonPath('$.identifiers.login').isEmpty()
                .jsonPath('$.networks[0]').isEqualTo('BTC')
                .jsonPath('$.networks[1]').isEqualTo('ETH')
                .jsonPath('$.algorithmVersion').isEmpty()
                .jsonPath('$.attributionCount').isEmpty()
    }

    def "a finished analysis keeps coverage, algorithm version and counts"() {
        given:
        def id = recorder.begin(request())

        when:
        recorder.finish(id, completed())

        then:
        client.get().uri("/api/1/analyses/${id}").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.status').isEqualTo('COMPLETED')
                .jsonPath('$.finishedAt').exists()
                .jsonPath('$.algorithmVersion').isEqualTo('v1')
                .jsonPath('$.attributionCount').isEqualTo(1)
                .jsonPath('$.excludedAddressCount').isEqualTo(1)
                .jsonPath('$.sources[0].sourceName').isEqualTo('forum')
                .jsonPath('$.sources[0].status').isEqualTo('CHECKED')
                .jsonPath('$.sources[0].mentionCount').isEqualTo(1)
        repository.findById(id).get().result.attributions*.candidate.mentionIds == [['m1']]
    }

    def "successive analyses of one case are independent records"() {
        when:
        def first = recorder.begin(request())
        def second = recorder.begin(request())
        recorder.finish(second, completed('v2'))

        then:
        first != second
        client.get().uri("/api/1/analyses/${first}").exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath('$.status').isEqualTo('RUNNING')
        client.get().uri("/api/1/analyses/${second}").exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath('$.status').isEqualTo('COMPLETED')
    }

    def "a later analysis does not change an earlier saved result"() {
        given:
        def first = recorder.begin(request())
        recorder.finish(first, completed('v1'))
        def before = body("/api/1/analyses/${first}")

        when:
        def second = recorder.begin(request(AnalysisTrigger.MANUAL, 'other-nick'))
        recorder.finish(second, failed())

        then:
        body("/api/1/analyses/${first}") == before
        body("/api/1/analyses/${first}") == before
        repository.findById(first).get().result.attributions.size() == 1
    }

    def "the case history lists every analysis newest first, failed and partial included"() {
        given:
        def first = recorder.begin(request())
        recorder.finish(first, completed())
        def second = recorder.begin(request())
        recorder.finish(second, failed())
        def third = recorder.begin(request())

        when:
        def history = client.get().uri("/api/1/cases/${caseId}/analyses").exchange()
                .expectStatus().isOk().expectBody()

        then:
        history.jsonPath('$.length()').isEqualTo(3)
                .jsonPath('$[0].id').isEqualTo(third)
                .jsonPath('$[0].status').isEqualTo('RUNNING')
                .jsonPath('$[1].id').isEqualTo(second)
                .jsonPath('$[1].status').isEqualTo('FAILED')
                .jsonPath('$[2].id').isEqualTo(first)
                .jsonPath('$[2].status').isEqualTo('COMPLETED')
    }

    def "a failed analysis keeps its failure and is not stored as zero findings"() {
        given:
        def id = recorder.begin(request())

        when:
        recorder.finish(id, failed())
        def saved = repository.findById(id).get()

        then:
        saved.status == AnalysisStatus.FAILED
        saved.finishedAt != null
        saved.result.failures*.detail == ['ALL_SOURCES_UNAVAILABLE']
        saved.result.limitations == [
            ReportLimitation.SOURCE_UNAVAILABLE,
            ReportLimitation.ANALYSIS_INCOMPLETE
        ]
        client.get().uri("/api/1/analyses/${id}").exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath('$.status').isEqualTo('FAILED')
                .jsonPath('$.sources[0].status').isEqualTo('UNAVAILABLE')
                .jsonPath('$.algorithmVersion').isEmpty()
    }

    def "a finished analysis cannot be finished again"() {
        given:
        def id = recorder.begin(request())
        recorder.finish(id, completed('v1'))

        when:
        recorder.finish(id, failed())

        then:
        thrown(IllegalStateException)
        repository.findById(id).get().status == AnalysisStatus.COMPLETED
        repository.findById(id).get().result.algorithmVersion == 'v1'
    }

    def "an unknown analysis is a Polish 404 and another case history is empty"() {
        expect:
        client.get().uri('/api/1/analyses/does-not-exist').exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath('$.message').isEqualTo('Nie znaleziono analizy o podanym identyfikatorze.')
        client.get().uri("/api/1/cases/other-${caseId}/analyses").exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath('$.length()').isEqualTo(0)
    }
}
