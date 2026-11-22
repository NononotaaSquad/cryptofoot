package squad.nononotaa.backend.service

import java.time.Instant
import java.util.concurrent.Executor
import org.springframework.beans.factory.annotation.Autowired
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AddressActivityCollector
import squad.nononotaa.backend.analysis.AnalysisOrchestrator
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.AnalysisResult
import squad.nononotaa.backend.analysis.CandidatePreparation
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.ConfidenceScoring
import squad.nononotaa.backend.analysis.ExclusionClassifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.MentionSearch
import squad.nononotaa.backend.analysis.SearchResult
import squad.nononotaa.backend.analysis.SourceReport
import squad.nononotaa.backend.exception.ConflictException
import squad.nononotaa.backend.exception.NotFoundException
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.AnalysisRepository

class AnalysisRerunSpec extends BaseSpec {

    @Autowired
    AnalysisRecorder recorder

    @Autowired
    AnalysisRepository repository

    @Autowired
    AnalysisHistoryService history

    String caseId = "case-${UUID.randomUUID()}"
    MentionSearch search = Stub()
    List<Runnable> deferred = []
    Executor deferring = { Runnable task -> deferred << task } as Executor
    def currentIdentifiers = [
        new CaseIdentifier(IdentifierType.LOGIN, 'new-login')
    ]
    def identifiers = { String id ->
        id == caseId ? currentIdentifiers : null
    } as CaseIdentifierSource

    private AnalysisRerunService rerunService() {
        def orchestrator = new AnalysisOrchestrator(search, Stub(CandidatePreparation), Stub(ExclusionClassifier), Stub(ConfidenceScoring), Stub(AddressActivityCollector), recorder)
        new AnalysisRerunService(orchestrator, repository, identifiers, history, deferring)
    }

    private String earlierCompleted() {
        def id = recorder.begin(new AnalysisRequest(caseId, AnalysisTrigger.MANUAL, [
            new CaseIdentifier(IdentifierType.NICKNAME, 'old-nick')
        ]))
        recorder.finish(id, new AnalysisResult(AnalysisStatus.COMPLETED, [], [], [
            new SourceReport('forum', SourceStatus.CHECKED, true, Instant.now())
        ], [], [], [], [], [], 'v1'))
        id
    }

    private String json(String id) {
        client.get().uri("/api/1/analyses/${id}").exchange().expectStatus().isOk().expectBody(String).returnResult().responseBody
    }

    def "a rerun creates a new manual Analysis with the current identifiers and keeps the earlier result while it runs"() {
        given:
        def earlier = earlierCompleted()
        def before = json(earlier)

        when:
        def started = rerunService().rerun(caseId)

        then:
        started.id != earlier
        started.status == AnalysisStatus.RUNNING
        started.trigger == AnalysisTrigger.MANUAL
        started.identifiers.login == 'new-login'
        started.identifiers.nickname == null
        json(earlier) == before

        when:
        search.search(_, _) >> new SearchResult([], [
            new SourceReport('forum', SourceStatus.CHECKED, true, Instant.now())
        ])
        deferred*.run()

        then:
        history.get(started.id).status == AnalysisStatus.COMPLETED
        json(earlier) == before
    }

    def "the search hears how many Analyses of this Case started before, whatever their trigger or outcome"() {
        given:
        earlierCompleted()
        recorder.begin(new AnalysisRequest(caseId, AnalysisTrigger.SCHEDULED, currentIdentifiers))
        recorder.begin(new AnalysisRequest("other-${caseId}", AnalysisTrigger.MANUAL, currentIdentifiers))
        repository.findByCaseIdOrderByStartedAtDescIdDesc(caseId).findAll {
            it.status == AnalysisStatus.RUNNING
        }.each {
            recorder.finish(it.id, new AnalysisResult(AnalysisStatus.FAILED, [], [], [], [], [], [], [], [], null))
        }
        def heard = []
        search.search(_, _) >> { List ids, int earlier ->
            heard << earlier
            new SearchResult([], [
                new SourceReport('forum', SourceStatus.CHECKED, true, Instant.now())
            ])
        }

        when:
        rerunService().rerun(caseId)
        deferred*.run()

        then:
        heard == [2]
    }

    def "a failed rerun is stored as failed and the earlier result stays untouched"() {
        given:
        def earlier = earlierCompleted()
        def before = json(earlier)
        search.search(_, _) >> { throw new IllegalStateException('boom') }

        when:
        def started = rerunService().rerun(caseId)
        deferred*.run()

        then:
        history.get(started.id).status == AnalysisStatus.FAILED
        json(earlier) == before
        history.listByCase(caseId)*.status == [
            AnalysisStatus.FAILED,
            AnalysisStatus.COMPLETED
        ]
    }

    def "a second rerun is refused while one is running"() {
        given:
        def service = rerunService()
        service.rerun(caseId)

        when:
        service.rerun(caseId)

        then:
        thrown(ConflictException)
        history.listByCase(caseId).size() == 1
    }

    def "a rerun of an unknown case is refused and stores nothing"() {
        when:
        rerunService().rerun("missing-${caseId}")

        then:
        thrown(NotFoundException)
        history.listByCase("missing-${caseId}").isEmpty()
    }
}
