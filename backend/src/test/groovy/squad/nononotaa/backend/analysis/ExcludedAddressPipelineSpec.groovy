package squad.nononotaa.backend.analysis

import static squad.nononotaa.backend.model.ContractCodeCheck.ACCOUNT
import static squad.nononotaa.backend.model.ContractCodeCheck.CONTRACT
import static squad.nononotaa.backend.model.ContractCodeCheck.UNAVAILABLE

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ExclusionLabel
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.service.ContractCodeSource
import squad.nononotaa.backend.service.ExclusionClassifierService
import squad.nononotaa.backend.service.ExclusionLabelDirectory

/** Issue #22 criteria 2 and 3 with the real classifier inside the orchestration. */
class ExcludedAddressPipelineSpec extends Specification {

    static final String EXCHANGE = '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0'
    static final String CONTRACT_ADDRESS = '0x7a250d5630b4cf539739df2c5dacb4c659f2488d'
    static final String PERSON = '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
    static final String UNCHECKED = '0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359'

    MentionSearch search = Stub()
    CandidatePreparation preparation = Stub()
    ConfidenceScoring scoring = Mock()
    AnalysisRecorder recorder = Mock()
    ContractCodeSource rpc = Stub() {
        getSourceName() >> 'rpc'
        check(CONTRACT_ADDRESS) >> CONTRACT
        check(PERSON) >> ACCOUNT
        check(UNCHECKED) >> UNAVAILABLE
    }
    def labels = new ExclusionLabelDirectory([
        (new CanonicalAddress(Network.ETH, EXCHANGE)): new ExclusionLabel(ExclusionReason.EXCHANGE, 'Kraken 4 [kraken]', 'Etherscan labels')
    ])
    def orchestrator = new AnalysisOrchestrator(search, preparation, new ExclusionClassifierService(labels, [rpc]), scoring, recorder)

    def exchange = new Candidate('ETH', EXCHANGE, ['m1'])
    def contract = new Candidate('ETH', CONTRACT_ADDRESS, ['m1'])
    def person = new Candidate('ETH', PERSON, ['m1'])
    def unchecked = new Candidate('ETH', UNCHECKED, ['m1'])
    List<Candidate> candidates = [
        exchange,
        contract,
        person,
        unchecked
    ]
    AnalysisResult finished

    def setup() {
        def mention = new FoundMention('m1', 'forum', 'https://example.test/m1', Instant.parse('2026-11-21T10:00:00Z'), 'ctx', [])
        search.search(_) >> new SearchResult([mention], [
            new SourceReport('forum', SourceStatus.CHECKED, true, Instant.parse('2026-11-21T10:00:00Z'))
        ])
        preparation.prepare(_) >> { new PreparedCandidates(candidates, []) }
        recorder.begin(_) >> 'a-1'
        recorder.finish('a-1', _) >> { id, result -> finished = result }
    }

    def "Excluded Addresses never reach scoring, never come back as Attributions, and an unchecked one stays distinguishable"() {
        when:
        orchestrator.run(new AnalysisRequest('case-1', AnalysisTrigger.MANUAL, []))

        then:
        1 * scoring.score({
            it.accepted == [person] && it.unclassified == [unchecked]
        }) >> new ScoringResult([
            new ScoredAttribution(person, 70, [
                new AppliedRule('forum-post', 70)
            ], '', ClassificationStatus.NOT_EXCLUDED),
            new ScoredAttribution(unchecked, 70, [
                new AppliedRule('forum-post', 70)
            ], '', ClassificationStatus.NOT_EXCLUDED)
        ], 'v1')
        finished.excluded.collectEntries {
            [(it.candidate.address): it.reason]
        } == [
            (EXCHANGE)        : ExclusionReason.EXCHANGE,
            (CONTRACT_ADDRESS): ExclusionReason.SMART_CONTRACT
        ]
        finished.excluded*.classificationSource == [
            'Etherscan labels: Kraken 4 [kraken]',
            'rpc: executable code present'
        ]
        finished.attributions*.candidate*.address == [PERSON, UNCHECKED]
        finished.attributions*.classificationStatus == [
            ClassificationStatus.NOT_EXCLUDED,
            ClassificationStatus.UNAVAILABLE
        ]
        finished.unclassified == [unchecked]
        finished.status == AnalysisStatus.PARTIAL
        finished.limitations.contains(ReportLimitation.CLASSIFICATION_UNAVAILABLE)
    }

    def "when every candidate is an Excluded Address the scoring is never called and the Analysis is completed"() {
        given:
        candidates = [exchange, contract]

        when:
        orchestrator.run(new AnalysisRequest('case-1', AnalysisTrigger.MANUAL, []))

        then:
        0 * scoring.score(_)
        finished.status == AnalysisStatus.COMPLETED
        finished.attributions.isEmpty()
        finished.unclassified.isEmpty()
        finished.excluded*.candidate == [exchange, contract]
        finished.excluded*.candidate*.mentionIds == [['m1'], ['m1']]
        finished.limitations.isEmpty()
    }

    def "with the on-chain check switched off only listed addresses are excluded and every other ETH Attribution is flagged"() {
        given:
        def offline = new AnalysisOrchestrator(search, preparation, new ExclusionClassifierService(labels, []), scoring, recorder)
        candidates = [exchange, person]

        when:
        offline.run(new AnalysisRequest('case-1', AnalysisTrigger.MANUAL, []))

        then:
        1 * scoring.score({
            it.accepted.isEmpty() && it.unclassified == [person]
        }) >> new ScoringResult([
            new ScoredAttribution(person, 70, [
                new AppliedRule('forum-post', 70)
            ], '', ClassificationStatus.NOT_EXCLUDED)
        ], 'v1')
        finished.excluded*.candidate == [exchange]
        finished.attributions*.classificationStatus == [
            ClassificationStatus.UNAVAILABLE
        ]
        finished.status == AnalysisStatus.PARTIAL
        finished.limitations.contains(ReportLimitation.CLASSIFICATION_UNAVAILABLE)
    }

    def "a crash inside the classifier fails the Analysis instead of producing unchecked Attributions"() {
        given:
        ContractCodeSource broken = Stub() {
            getSourceName() >> 'broken'
            check(_) >> { throw new IllegalStateException('bug') }
        }
        def crashing = new AnalysisOrchestrator(search, preparation, new ExclusionClassifierService(labels, [broken]), scoring, recorder)
        candidates = [exchange, person]

        when:
        def run = crashing.run(new AnalysisRequest('case-1', AnalysisTrigger.MANUAL, []))

        then:
        0 * scoring.score(_)
        run.status == AnalysisStatus.FAILED
        finished.status == AnalysisStatus.FAILED
        finished.failures == [
            new StageFailure(AnalysisStage.EXCLUSION, 'MODULE_ERROR')
        ]
        finished.attributions.isEmpty()
        finished.excluded.isEmpty()
    }
}
