package squad.nononotaa.backend.service

import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AnalysisOrchestrator
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.CandidatePreparation
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.ConfidenceScoring
import squad.nononotaa.backend.analysis.ExclusionClassifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.MentionSearch
import squad.nononotaa.backend.analysis.PremiseAssessment
import squad.nononotaa.backend.analysis.SearchResult
import squad.nononotaa.backend.analysis.SourceReport
import squad.nononotaa.backend.config.SchedulerProperties
import squad.nononotaa.backend.dto.UpdateScheduleRequest
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.AssessmentMode
import squad.nononotaa.backend.model.ScheduleAttemptOutcome as Outcome
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.repository.CaseRepository
import squad.nononotaa.backend.repository.ScheduleRepository
import squad.nononotaa.testsupport.MutableClock

class ScheduledRunnerSpec extends BaseSpec {

    @Autowired
    ScheduleRepository schedules

    @Autowired
    AnalysisRepository analyses

    @Autowired
    AnalysisRecorder recorder

    @Autowired
    AnalysisHistoryService history

    static final Instant T0 = Instant.parse('2026-11-21T04:00:00Z')
    static final Instant DUE = Instant.parse('2026-11-21T05:00:00Z')

    String caseId = "case-${UUID.randomUUID()}"
    String otherCase = "other-${UUID.randomUUID()}"
    def clock = new MutableClock(T0)
    def known = [] as Set
    MentionSearch search = Stub() {
        search(_) >> new SearchResult([], [
            new SourceReport('forum', SourceStatus.CHECKED, true, T0)
        ])
    }
    CaseRepository cases = Stub() {
        existsById(_) >> true
    }
    Executor sync = { Runnable task -> task.run() } as Executor
    def identifiers = { String id ->
        known.contains(id) ? [
            new CaseIdentifier(IdentifierType.NICKNAME, 'nick')
        ] : null
    } as CaseIdentifierSource

    private ScheduleService service() {
        new ScheduleService(schedules, new ScheduleCalculator(), clock, new SchedulerProperties(false, Duration.ofMinutes(2)), cases)
    }

    private ScheduledRunner runner(boolean wired = true) {
        def orchestrator = new AnalysisOrchestrator(search, Stub(CandidatePreparation), Stub(ExclusionClassifier), Stub(PremiseAssessment) {
            getMode() >> AssessmentMode.RULES_ONLY
        }, Stub(ConfidenceScoring), recorder)
        def rerun = new AnalysisRerunService(orchestrator, analyses, identifiers, history, sync)
        ObjectProvider<AnalysisRerunService> provider = Stub() {
            getIfAvailable() >> (wired ? rerun : null)
        }
        new ScheduledRunner(schedules, provider, new ScheduleCalculator(), clock, new SchedulerProperties(false, Duration.ofMinutes(2)))
    }

    private enable(String id) {
        known << id
        service().update(id, new UpdateScheduleRequest(true, SchedulePreset.DAILY))
    }

    def "a due schedule starts a scheduled Analysis through the shared flow and moves to the next term"() {
        given:
        enable(caseId)
        clock.now = DUE.plusSeconds(30)

        when:
        runner().runDue()
        def schedule = schedules.findById(caseId).get()
        def saved = history.listByCase(caseId)

        then:
        saved.size() == 1
        saved[0].trigger == AnalysisTrigger.SCHEDULED
        saved[0].status == AnalysisStatus.COMPLETED
        schedule.lastAttempt.outcome == Outcome.STARTED
        schedule.lastAttempt.analysisId == saved[0].id
        schedule.nextExecutionAt == Instant.parse('2026-11-22T05:00:00Z')
    }

    def "nothing runs before the term, for a disabled schedule, or twice for the same term"() {
        given:
        enable(caseId)
        known << otherCase
        service().update(otherCase, new UpdateScheduleRequest(true, null))
        service().update(otherCase, new UpdateScheduleRequest(false, null))

        when:
        clock.now = DUE.minusSeconds(1)
        runner().runDue()

        then:
        history.listByCase(caseId).isEmpty()

        when:
        clock.now = DUE.plusSeconds(1)
        runner().runDue()
        runner().runDue()

        then:
        history.listByCase(caseId).size() == 1
        history.listByCase(otherCase).isEmpty()
    }

    def "an overlapping term is skipped, recorded and starts no second Analysis"() {
        given:
        enable(caseId)
        recorder.begin(new AnalysisRequest(caseId, AnalysisTrigger.MANUAL, [
            new CaseIdentifier(IdentifierType.NICKNAME, 'nick')
        ]))
        clock.now = DUE.plusSeconds(5)

        when:
        runner().runDue()
        def schedule = schedules.findById(caseId).get()

        then:
        history.listByCase(caseId).size() == 1
        schedule.lastAttempt.outcome == Outcome.SKIPPED_OVERLAP
        schedule.lastAttempt.analysisId == null
        schedule.nextExecutionAt == Instant.parse('2026-11-22T05:00:00Z')
    }

    def "a failed attempt is recorded and visible and does not block other cases"() {
        given:
        enable(caseId)
        enable(otherCase)
        known.remove(caseId)
        clock.now = DUE.plusSeconds(5)

        when:
        runner().runDue()

        then:
        schedules.findById(caseId).get().lastAttempt.outcome == Outcome.FAILED
        schedules.findById(caseId).get().nextExecutionAt == Instant.parse('2026-11-22T05:00:00Z')
        history.listByCase(caseId).isEmpty()
        schedules.findById(otherCase).get().lastAttempt.outcome == Outcome.STARTED
        client.get().uri("/api/1/cases/${caseId}/schedule").exchange().expectStatus().isOk().expectBody()
                .jsonPath('$.lastAttempt.outcome').isEqualTo('FAILED')
    }

    def "terms missed during downtime are not run or made up, and the skip is recorded"() {
        given:
        enable(caseId)
        clock.now = DUE.plus(Duration.ofDays(3)).plusSeconds(600)

        when:
        runner().runDue()
        runner().runDue()
        def schedule = schedules.findById(caseId).get()

        then:
        history.listByCase(caseId).isEmpty()
        schedule.lastAttempt.outcome == Outcome.SKIPPED_DOWNTIME
        schedule.nextExecutionAt == Instant.parse('2026-11-25T05:00:00Z')
    }

    def "the scheduler stays idle and changes nothing while the Analysis flow is not wired"() {
        given:
        enable(caseId)
        def before = schedules.findById(caseId).get()
        clock.now = DUE.plusSeconds(30)

        when:
        runner(false).runDue()

        then:
        schedules.findById(caseId).get() == before
        history.listByCase(caseId).isEmpty()
    }
}
