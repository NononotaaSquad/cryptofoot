package squad.nononotaa.backend.controller

import groovy.json.JsonSlurper
import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AnalysisResult
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.ExcludedCandidate
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.model.AnalysisRecord
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.repository.AnalysisRepository

class CaseListSummarySpec extends BaseSpec {

    @Autowired
    AnalysisRepository repository

    def t0 = Instant.parse('2026-11-22T10:00:00Z')

    private Map json(String uri) {
        new JsonSlurper().parseText(client.get().uri(uri).exchange().expectStatus().isOk()
                .expectBody(String).returnResult().responseBody) as Map
    }

    private Map listed(String caseId) {
        (new JsonSlurper().parseText(client.get().uri('/api/1/cases').exchange().expectStatus().isOk()
                .expectBody(String).returnResult().responseBody) as List).find {
                    it.id == caseId
                } as Map
    }

    private static ScoredAttribution scored(int level) {
        new ScoredAttribution(new Candidate('ETH', "0x${level}", ['m1']), level, [])
    }

    private String save(String caseId, AnalysisStatus status, Instant startedAt, AnalysisResult result = null, String id = null) {
        repository.save(new AnalysisRecord(id, null, caseId, AnalysisTrigger.MANUAL, status, startedAt,
                status == AnalysisStatus.RUNNING ? null : startedAt.plusSeconds(5), [], [Network.ETH], result)).id
    }

    private static AnalysisResult result(AnalysisStatus status, List<Integer> levels, List excluded = []) {
        new AnalysisResult(status, [], [], [], [], [], excluded, [], levels.collect {
            scored(it)
        }, 'v1')
    }

    def "a new Case has no analyses, and the list returns it with both fields null"() {
        given:
        def caseId = createCase()

        expect:
        json("/api/1/cases/${caseId}").with {
            assert latestAnalysis == null
            assert latestFinishedAnalysis == null
            true
        }
        listed(caseId).with {
            assert latestAnalysis == null
            assert latestFinishedAnalysis == null
            true
        }
    }

    def "creating a Case answers with both analyses null"() {
        expect:
        client.post().uri('/api/1/cases').contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body([nickname: "n-${UUID.randomUUID()}".toString()]).exchange()
                .expectStatus().isCreated().expectBody()
                .jsonPath('$.latestAnalysis').isEmpty()
                .jsonPath('$.latestFinishedAnalysis').isEmpty()
    }

    def "a newer RUNNING analysis stays latest while the older terminal one is latestFinished with its own snapshot"() {
        given:
        def caseId = createCase()
        def finished = save(caseId, AnalysisStatus.COMPLETED, t0, result(AnalysisStatus.COMPLETED, [40, 85, 10]))
        def running = save(caseId, AnalysisStatus.RUNNING, t0.plusSeconds(60))

        when:
        def details = json("/api/1/cases/${caseId}")
        def fromList = client.get().uri('/api/1/cases').exchange().expectStatus().isOk()
                .expectBody(String).returnResult().responseBody
        def listed = (new JsonSlurper().parseText(fromList) as List).find {
            it.id == caseId
        }

        then:
        details.latestAnalysis.id == running
        details.latestAnalysis.status == 'RUNNING'
        details.latestAnalysis.highestConfidenceLevel == null
        details.latestAnalysis.attributionCount == null
        details.latestFinishedAnalysis.id == finished
        details.latestFinishedAnalysis.highestConfidenceLevel == 85
        details.latestFinishedAnalysis.attributionCount == 3
        listed.latestAnalysis.id == running
        listed.latestFinishedAnalysis.id == finished
        listed.latestFinishedAnalysis.highestConfidenceLevel == 85
    }

    def "a RUNNING-only Case has no finished analysis"() {
        given:
        def caseId = createCase()
        def running = save(caseId, AnalysisStatus.RUNNING, t0)

        expect:
        json("/api/1/cases/${caseId}").with {
            assert latestAnalysis.id == running
            assert latestFinishedAnalysis == null
            true
        }
    }

    def "a terminal latest analysis is the same one in both fields, FAILED included"() {
        given:
        def caseId = createCase()
        save(caseId, AnalysisStatus.COMPLETED, t0, result(AnalysisStatus.COMPLETED, [90]))
        def failed = save(caseId, AnalysisStatus.FAILED, t0.plusSeconds(60), result(AnalysisStatus.FAILED, []))

        when:
        def details = json("/api/1/cases/${caseId}")

        then:
        details.latestAnalysis.id == failed
        details.latestFinishedAnalysis == details.latestAnalysis
        details.latestFinishedAnalysis.status == 'FAILED'
        details.latestFinishedAnalysis.highestConfidenceLevel == null
    }

    def "each terminal status is a finished analysis"() {
        given:
        def caseId = createCase()
        def id = save(caseId, status, t0, result(status, [20]))

        expect:
        json("/api/1/cases/${caseId}").latestFinishedAnalysis.id == id

        where:
        status << [
            AnalysisStatus.COMPLETED,
            AnalysisStatus.PARTIAL,
            AnalysisStatus.FAILED
        ]
    }

    def "no result, an empty result, all zeros and several values are distinct maxima"() {
        given:
        def caseId = createCase()
        save(caseId, AnalysisStatus.COMPLETED, t0, saved)

        expect:
        def analysis = json("/api/1/cases/${caseId}").latestAnalysis
        analysis.highestConfidenceLevel == maximum
        analysis.attributionCount == count

        where:
        saved || maximum | count
        null || null    | null
        result(AnalysisStatus.COMPLETED, []) || null    | 0
        result(AnalysisStatus.COMPLETED, [0, 0]) || 0       | 2
        result(AnalysisStatus.COMPLETED, [0, 55, 80, 79]) || 80      | 4
    }

    def "excluded addresses do not count towards the maximum or the attribution count"() {
        given:
        def caseId = createCase()
        def excluded = [
            new ExcludedCandidate(new Candidate('ETH', '0xdef', ['m1']), ExclusionReason.EXCHANGE, 'own-list')
        ]
        save(caseId, AnalysisStatus.COMPLETED, t0, result(AnalysisStatus.COMPLETED, [30], excluded))

        expect:
        json("/api/1/cases/${caseId}").latestAnalysis.with {
            assert highestConfidenceLevel == 30
            assert attributionCount == 1
            assert excludedAddressCount == 1
            true
        }
    }

    def "a legacy record without attributions keeps a null maximum and is not rescored"() {
        given:
        def caseId = createCase()
        def id = save(caseId, AnalysisStatus.COMPLETED, t0, new AnalysisResult(AnalysisStatus.COMPLETED, [], [], [], [], [], [], [], [], null))

        expect:
        json("/api/1/cases/${caseId}").latestFinishedAnalysis.with {
            assert it.id == id
            assert highestConfidenceLevel == null
            assert algorithmVersion == null
            true
        }
    }

    def "the same startedAt is ordered by id, for latest and for latestFinished"() {
        given:
        def caseId = createCase()
        def prefix = UUID.randomUUID().toString().replace('-', '')
        save(caseId, AnalysisStatus.COMPLETED, t0, result(AnalysisStatus.COMPLETED, [10]), "${prefix}1")
        save(caseId, AnalysisStatus.COMPLETED, t0, result(AnalysisStatus.COMPLETED, [20]), "${prefix}2")
        save(caseId, AnalysisStatus.RUNNING, t0, null, "${prefix}3")

        when:
        def details = json("/api/1/cases/${caseId}")

        then:
        details.latestAnalysis.id == "${prefix}3"
        details.latestFinishedAnalysis.id == "${prefix}2"
    }

    def "reading cases neither starts an analysis nor changes the stored ones"() {
        given:
        def caseId = createCase()
        save(caseId, AnalysisStatus.COMPLETED, t0, result(AnalysisStatus.COMPLETED, [50]))
        save(caseId, AnalysisStatus.RUNNING, t0.plusSeconds(60))
        def before = repository.findByCaseIdOrderByStartedAtDescIdDesc(caseId)

        when:
        json("/api/1/cases/${caseId}")
        client.get().uri('/api/1/cases').exchange().expectStatus().isOk()

        then:
        repository.findByCaseIdOrderByStartedAtDescIdDesc(caseId) == before
    }
}
