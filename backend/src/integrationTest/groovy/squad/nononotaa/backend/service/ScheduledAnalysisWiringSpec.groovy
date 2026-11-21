package squad.nononotaa.backend.service

import java.time.Duration
import java.time.Instant
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestPropertySource
import spock.util.concurrent.PollingConditions
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.config.SchedulerProperties
import squad.nononotaa.backend.dto.UpdateScheduleRequest
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.ScheduleAttemptOutcome
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.repository.CaseRepository
import squad.nononotaa.backend.repository.ScheduleRepository
import squad.nononotaa.testsupport.MutableClock

/** Scheduled runs use the same wired flow as the manual start; the on-chain check is off, so nothing leaves the app. */
@TestPropertySource(properties = 'app.exclusion.onchain-check-enabled=false')
class ScheduledAnalysisWiringSpec extends BaseSpec {

    @Autowired
    ObjectProvider<AnalysisRerunService> rerun

    @Autowired
    ScheduleRepository schedules

    @Autowired
    CaseRepository cases

    @Autowired
    AnalysisRepository analyses

    @Autowired
    ApplicationContext context

    def clock = new MutableClock(Instant.parse('2026-11-21T04:00:00Z'))
    def props = new SchedulerProperties(false, Duration.ofMinutes(2))

    def "the application wires one Analysis flow, and the background tick is switched off in specs"() {
        expect:
        rerun.getIfAvailable() != null
        context.getBeansOfType(ScheduledRunner).isEmpty()
    }

    def "a due term starts the real Analysis flow with trigger SCHEDULED and records the attempt"() {
        given:
        def caseId = createCase([nickname: 'zenek_fx'])
        new ScheduleService(schedules, new ScheduleCalculator(), clock, props, cases).update(caseId, new UpdateScheduleRequest(true, SchedulePreset.DAILY))
        clock.now = Instant.parse('2026-11-21T05:00:10Z')

        when:
        new ScheduledRunner(schedules, rerun, new ScheduleCalculator(), clock, props).runDue()
        def attempt = schedules.findById(caseId).get().lastAttempt

        then:
        attempt.outcome == ScheduleAttemptOutcome.STARTED
        new PollingConditions(timeout: 15, delay: 0.1).eventually {
            assert analyses.findById(attempt.analysisId).get().status != AnalysisStatus.RUNNING
        }

        when:
        def saved = analyses.findById(attempt.analysisId).get()

        then:
        saved.trigger == AnalysisTrigger.SCHEDULED
        saved.status == AnalysisStatus.PARTIAL
        saved.result.attributions.size() == 4
        saved.result.excluded.isEmpty()
        saved.result.attributions.findAll {
            it.candidate.network == 'ETH'
        }*.classificationStatus == [
            ClassificationStatus.UNAVAILABLE,
            ClassificationStatus.UNAVAILABLE
        ]
        saved.result.unclassified*.address.toSet() == [
            '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed',
            '0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae'
        ] as Set
        // the unchecked contract is no demo address and the live explorers are off: its activity is unavailable, not zero
        saved.result.limitations == [
            ReportLimitation.SOURCE_UNAVAILABLE,
            ReportLimitation.CLASSIFICATION_UNAVAILABLE,
            ReportLimitation.BLOCKCHAIN_DATA_UNAVAILABLE,
            ReportLimitation.ANALYSIS_INCOMPLETE
        ]
    }
}
