package squad.nononotaa.backend.service

import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AddressActivityCollector
import squad.nononotaa.backend.analysis.AnalysisOrchestrator
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.CandidatePreparation
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.ConfidenceScoring
import squad.nononotaa.backend.analysis.ExclusionClassifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.MentionSearch
import squad.nononotaa.backend.analysis.SearchResult
import squad.nononotaa.backend.analysis.SourceReport
import squad.nononotaa.backend.config.SchedulerProperties
import squad.nononotaa.backend.dto.UpdateScheduleRequest
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.ScheduleAttemptOutcome
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.repository.CaseRepository
import squad.nononotaa.backend.repository.ScheduleRepository
import squad.nononotaa.testsupport.MutableClock

class DemoSchedulerCheckSpec extends BaseSpec {

    @Autowired
    ScheduleRepository schedules

    @Autowired
    AnalysisRepository analyses

    @Autowired
    AnalysisRecorder recorder

    @Autowired
    AnalysisHistoryService history

    String caseId = "demo-check-${UUID.randomUUID()}"
    def clock = new MutableClock(Instant.parse('2026-11-21T10:00:20Z'))
    def props = new SchedulerProperties(true, Duration.ofMinutes(2))
    MentionSearch search = Stub() {
        search(_, _) >> new SearchResult([], [
            new SourceReport('stub-source', SourceStatus.CHECKED, true, Instant.now())
        ])
    }
    CaseRepository cases = Stub() {
        existsById(_) >> true
    }
    Executor sync = { Runnable task -> task.run() } as Executor
    def identifiers = { String id ->
        id == caseId ? [
            new CaseIdentifier(IdentifierType.NICKNAME, 'nick')
        ] : null
    } as CaseIdentifierSource

    private ScheduledRunner runner() {
        def orchestrator = new AnalysisOrchestrator(search, Stub(CandidatePreparation), Stub(ExclusionClassifier), Stub(ConfidenceScoring), Stub(AddressActivityCollector), recorder)
        def rerun = new AnalysisRerunService(orchestrator, analyses, identifiers, history, sync)
        ObjectProvider<AnalysisRerunService> provider = Stub() {
            getIfAvailable() >> rerun
        }
        new ScheduledRunner(schedules, provider, new ScheduleCalculator(), clock, props)
    }

    private tick(String time) {
        clock.now = Instant.parse(time)
        runner().runDue()
    }

    def "demo cadence: each minute creates a new saved scheduled Analysis and disabling stops further ones"() {
        given:
        def service = new ScheduleService(schedules, new ScheduleCalculator(), clock, props, cases)
        def enabled = service.update(caseId, new UpdateScheduleRequest(true, SchedulePreset.DEMO_EVERY_MINUTE))

        expect:
        enabled.demo
        enabled.nextExecutionAt == Instant.parse('2026-11-21T10:01:00Z')

        when:
        tick('2026-11-21T10:01:05Z')
        def first = history.listByCase(caseId)

        then:
        first.size() == 1
        first[0].trigger == AnalysisTrigger.SCHEDULED
        first[0].status == AnalysisStatus.COMPLETED

        when:
        tick('2026-11-21T10:02:05Z')
        def second = history.listByCase(caseId)

        then:
        second.size() == 2
        second*.trigger == [
            AnalysisTrigger.SCHEDULED,
            AnalysisTrigger.SCHEDULED
        ]
        second*.id.toSet().size() == 2
        second[1] == first[0]
        schedules.findById(caseId).get().lastAttempt.outcome == ScheduleAttemptOutcome.STARTED

        when:
        def disabled = service.update(caseId, new UpdateScheduleRequest(false, null))
        tick('2026-11-21T10:03:05Z')
        tick('2026-11-21T10:04:05Z')

        then:
        !disabled.enabled
        disabled.nextExecutionAt == null
        history.listByCase(caseId).size() == 2
    }
}