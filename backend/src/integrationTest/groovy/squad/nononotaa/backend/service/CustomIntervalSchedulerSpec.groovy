package squad.nononotaa.backend.service

import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor
import org.bson.Document
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
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
import squad.nononotaa.backend.analysis.PremiseAssessment
import squad.nononotaa.backend.analysis.SearchResult
import squad.nononotaa.backend.analysis.SourceReport
import squad.nononotaa.backend.config.SchedulerProperties
import squad.nononotaa.backend.dto.UpdateScheduleRequest
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.AssessmentMode
import squad.nononotaa.backend.model.ScheduleAttemptOutcome as Outcome
import squad.nononotaa.backend.model.ScheduleIntervalUnit
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.repository.CaseRepository
import squad.nononotaa.backend.repository.ScheduleRepository
import squad.nononotaa.testsupport.MutableClock

class CustomIntervalSchedulerSpec extends BaseSpec {

    @Autowired
    ScheduleRepository schedules

    @Autowired
    AnalysisRepository analyses

    @Autowired
    AnalysisRecorder recorder

    @Autowired
    AnalysisHistoryService history

    @Autowired
    MongoTemplate mongo

    static final Instant T0 = Instant.parse('2026-11-21T10:00:20Z')

    String caseId = "interval-${UUID.randomUUID()}"
    def clock = new MutableClock(T0)
    def props = new SchedulerProperties(false, Duration.ofMinutes(2))
    def queued = []
    boolean runInline = true
    MentionSearch search = Stub() {
        search(_, _) >> new SearchResult([], [
            new SourceReport('synthetic', SourceStatus.CHECKED, true, T0)
        ])
    }
    CaseRepository cases = Stub() {
        existsById(_) >> true
    }
    Executor executor = { Runnable task ->
        runInline ? task.run() : queued << task
    } as Executor
    def identifiers = { String id ->
        id == caseId ? [
            new CaseIdentifier(IdentifierType.NICKNAME, 'nick')
        ] : null
    } as CaseIdentifierSource

    private ScheduleService service() {
        new ScheduleService(schedules, new ScheduleCalculator(), clock, props, cases)
    }

    private ScheduledRunner runner() {
        def orchestrator = new AnalysisOrchestrator(search, Stub(CandidatePreparation), Stub(ExclusionClassifier), Stub(PremiseAssessment) {
            getMode() >> AssessmentMode.RULES_ONLY
        }, Stub(ConfidenceScoring), Stub(AddressActivityCollector), recorder)
        def rerun = new AnalysisRerunService(orchestrator, analyses, identifiers, history, executor)
        ObjectProvider<AnalysisRerunService> provider = Stub() {
            getIfAvailable() >> rerun
        }
        new ScheduledRunner(schedules, provider, new ScheduleCalculator(), clock, props)
    }

    private tick(String time) {
        clock.now = Instant.parse(time)
        runner().runDue()
    }

    private enableCustom(long value, ScheduleIntervalUnit unit) {
        service().update(caseId, new UpdateScheduleRequest(true, SchedulePreset.CUSTOM_INTERVAL, value, unit))
    }

    def "every 1 minute: consecutive terms start scheduled Analyses on a fixed grid, disabling stops them"() {
        when:
        def enabled = enableCustom(1, ScheduleIntervalUnit.MINUTES)

        then:
        enabled.nextExecutionAt == Instant.parse('2026-11-21T10:01:20Z')

        when:
        tick('2026-11-21T10:01:27Z')

        then:
        history.listByCase(caseId).size() == 1
        schedules.findById(caseId).get().nextExecutionAt == Instant.parse('2026-11-21T10:02:20Z')

        when:
        tick('2026-11-21T10:01:50Z')

        then:
        history.listByCase(caseId).size() == 1

        when:
        tick('2026-11-21T10:02:29Z')
        def saved = history.listByCase(caseId)

        then:
        saved.size() == 2
        saved*.trigger == [
            AnalysisTrigger.SCHEDULED,
            AnalysisTrigger.SCHEDULED
        ]
        saved*.status.toSet() == [AnalysisStatus.COMPLETED].toSet()
        schedules.findById(caseId).get().nextExecutionAt == Instant.parse('2026-11-21T10:03:20Z')

        when:
        service().update(caseId, new UpdateScheduleRequest(false))
        tick('2026-11-21T10:03:30Z')
        tick('2026-11-21T10:04:30Z')

        then:
        history.listByCase(caseId).size() == 2
        schedules.findById(caseId).get().nextExecutionAt == null
    }

    def "changing the interval of a running schedule recomputes the term from now"() {
        given:
        enableCustom(1, ScheduleIntervalUnit.MINUTES)
        clock.now = Instant.parse('2026-11-21T10:00:50Z')

        when:
        def changed = enableCustom(2, ScheduleIntervalUnit.HOURS)

        then:
        changed.nextExecutionAt == Instant.parse('2026-11-21T12:00:50Z')

        when:
        tick('2026-11-21T10:05:00Z')

        then:
        history.listByCase(caseId).isEmpty()
    }

    def "an Analysis longer than the interval skips the overlapping terms without building a queue"() {
        given:
        runInline = false
        enableCustom(1, ScheduleIntervalUnit.MINUTES)

        when:
        tick('2026-11-21T10:01:21Z')
        tick('2026-11-21T10:02:21Z')
        tick('2026-11-21T10:03:21Z')

        then:
        history.listByCase(caseId).size() == 1
        history.listByCase(caseId)[0].status == AnalysisStatus.RUNNING
        queued.size() == 1
        schedules.findById(caseId).get().lastAttempt.outcome == Outcome.SKIPPED_OVERLAP
        schedules.findById(caseId).get().nextExecutionAt == Instant.parse('2026-11-21T10:04:20Z')

        when:
        queued.each { it.run() }
        tick('2026-11-21T10:04:21Z')
        def saved = history.listByCase(caseId)

        then:
        saved.size() == 2
        saved*.trigger.toSet() == [AnalysisTrigger.SCHEDULED].toSet()
        schedules.findById(caseId).get().lastAttempt.outcome == Outcome.STARTED
    }

    def "two instances ticking at the same moment start one Analysis for the term"() {
        given:
        enableCustom(1, ScheduleIntervalUnit.MINUTES)
        clock.now = Instant.parse('2026-11-21T10:01:25Z')
        def a = runner()
        def b = runner()

        when:
        def threads = [a, b].collect { r -> Thread.start { r.runDue() } }
        threads*.join()

        then:
        history.listByCase(caseId).size() == 1
        schedules.findById(caseId).get().nextExecutionAt == Instant.parse('2026-11-21T10:02:20Z')
    }

    def "a schedule stored before custom intervals existed still loads and runs"() {
        given:
        mongo.getCollection('schedules').insertOne(new Document([
            _id              : caseId,
            _class           : 'squad.nononotaa.backend.model.ScheduleRecord',
            version          : 0L,
            enabled          : true,
            cadence          : 'DEMO_EVERY_MINUTE',
            nextExecutionAt  : Date.from(Instant.parse('2026-11-21T10:01:00Z'))
        ]))

        when:
        def read = service().get(caseId)
        tick('2026-11-21T10:01:05Z')

        then:
        read.enabled
        read.cadence == SchedulePreset.DEMO_EVERY_MINUTE
        read.intervalValue == null
        read.intervalUnit == null
        history.listByCase(caseId).size() == 1
        schedules.findById(caseId).get().nextExecutionAt == Instant.parse('2026-11-21T10:02:00Z')
    }

    def "the saved interval survives a restart: a fresh service and runner read it from storage"() {
        given:
        enableCustom(15, ScheduleIntervalUnit.MINUTES)

        when:
        def read = new ScheduleService(schedules, new ScheduleCalculator(), clock, props, cases).get(caseId)
        tick('2026-11-21T10:15:21Z')

        then:
        read.cadence == SchedulePreset.CUSTOM_INTERVAL
        read.intervalValue == 15
        read.intervalUnit == ScheduleIntervalUnit.MINUTES
        history.listByCase(caseId).size() == 1
        schedules.findById(caseId).get().nextExecutionAt == Instant.parse('2026-11-21T10:30:20Z')
    }
}
