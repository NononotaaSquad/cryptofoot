package squad.nononotaa.backend.controller

import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.AnalysisResult
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.SourceReport
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.SourceStatus

class AnalysisComparisonSpec extends BaseSpec {

    @Autowired
    AnalysisRecorder recorder

    String caseId = "case-${UUID.randomUUID()}"
    def at = Instant.parse('2026-11-21T10:00:00Z')

    private String analysis(String forCase, AnalysisResult result, String nickname = 'nick') {
        def id = recorder.begin(new AnalysisRequest(forCase, AnalysisTrigger.MANUAL, [
            new CaseIdentifier(IdentifierType.NICKNAME, nickname)
        ]))
        if (result) {
            recorder.finish(id, result)
        }
        Thread.sleep(5)
        id
    }

    private AnalysisResult result(List<ScoredAttribution> attributions, SourceStatus source = SourceStatus.CHECKED,
            AnalysisStatus status = AnalysisStatus.COMPLETED, String version = 'v1') {
        new AnalysisResult(status, [], [], [
            new SourceReport('forum', source, true, at)
        ],
        [
            new FoundMention('m1', 'forum', 'https://example.test/m1', at, null, ['0xaaa'])
        ], [], [], [], attributions, version)
    }

    private ScoredAttribution attribution(int level) {
        new ScoredAttribution(new Candidate('ETH', '0xaaa', ['m1']), level, [], '', ClassificationStatus.NOT_EXCLUDED)
    }

    private compare(String earlier, String later, String forCase = caseId) {
        client.get().uri("/api/1/cases/${forCase}/comparison?earlierAnalysisId=${earlier}&laterAnalysisId=${later}").exchange()
    }

    def "compares two analyses of one case and keeps both analyses and their evidence"() {
        given:
        def earlier = analysis(caseId, result([attribution(50)]))
        def later = analysis(caseId, result([attribution(65)], SourceStatus.CHECKED, AnalysisStatus.COMPLETED, 'v2'))

        expect:
        compare(earlier, later).expectStatus().isOk()
                .expectBody()
                .jsonPath('$.earlierAnalysis.id').isEqualTo(earlier)
                .jsonPath('$.laterAnalysis.id').isEqualTo(later)
                .jsonPath('$.changes.length()').isEqualTo(1)
                .jsonPath('$.changes[0].change').isEqualTo('CONFIDENCE_LEVEL_CHANGED')
                .jsonPath('$.changes[0].network').isEqualTo('ETH')
                .jsonPath('$.changes[0].address').isEqualTo('0xaaa')
                .jsonPath('$.changes[0].earlierConfidenceLevel').isEqualTo(50)
                .jsonPath('$.changes[0].laterConfidenceLevel').isEqualTo(65)
                .jsonPath('$.changes[0].earlierMentionIds[0]').isEqualTo('m1')
                .jsonPath('$.changes[0].laterMentionIds[0]').isEqualTo('m1')
                .jsonPath('$.comparabilityLimitations[0]').isEqualTo('ALGORITHM_VERSION_DIFFERS')
    }

    def "an apparent absence is flagged as unverifiable when the later source was unavailable"() {
        given:
        def earlier = analysis(caseId, result([attribution(70)]))
        def later = analysis(caseId, result([], SourceStatus.UNAVAILABLE, AnalysisStatus.PARTIAL))

        expect:
        compare(earlier, later).expectStatus().isOk()
                .expectBody()
                .jsonPath('$.changes[0].change').isEqualTo('NOT_OBSERVED')
                .jsonPath('$.changes[0].absenceVerifiable').isEqualTo(false)
                .jsonPath('$.comparabilityLimitations[0]').isEqualTo('SOURCE_COVERAGE_DIFFERS')
                .jsonPath('$.comparabilityLimitations[1]').isEqualTo('PARTIAL_ANALYSIS')
    }

    def "the comparison reads saved results only and changes nothing"() {
        given:
        def earlier = analysis(caseId, result([attribution(50)]))
        def later = analysis(caseId, result([attribution(65)]))
        def before = client.get().uri("/api/1/cases/${caseId}/analyses").exchange().expectBody(String).returnResult().responseBody

        when:
        compare(earlier, later).expectStatus().isOk()

        then:
        client.get().uri("/api/1/cases/${caseId}/analyses").exchange().expectBody(String).returnResult().responseBody == before
    }

    def "analyses of different cases cannot be compared"() {
        given:
        def mine = analysis(caseId, result([attribution(50)]))
        def foreign = analysis("other-${caseId}", result([attribution(50)]))

        expect:
        compare(mine, foreign).expectStatus().isBadRequest()
                .expectBody().jsonPath('$.message').isEqualTo('Porównać można tylko analizy tej samej sprawy.')
        compare(foreign, mine, "other-${caseId}").expectStatus().isBadRequest()
    }

    def "an unknown analysis is 404, a running one is 409 and a reversed order is 400"() {
        given:
        def first = analysis(caseId, result([attribution(50)]))
        def second = analysis(caseId, result([attribution(60)]))
        def running = analysis(caseId, null)

        expect:
        compare(first, 'does-not-exist').expectStatus().isNotFound()
        compare(first, running).expectStatus().isEqualTo(409)
        compare(second, first).expectStatus().isBadRequest()
                .expectBody().jsonPath('$.message').isEqualTo('Analiza wcześniejsza musi rozpocząć się przed późniejszą.')
    }
}
