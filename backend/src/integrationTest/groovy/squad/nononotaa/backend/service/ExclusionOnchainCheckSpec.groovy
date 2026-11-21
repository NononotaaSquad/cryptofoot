package squad.nononotaa.backend.service

import java.time.Duration
import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AnalysisOrchestrator
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.AnalysisResult
import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CandidatePreparation
import squad.nononotaa.backend.analysis.ConfidenceScoring
import squad.nononotaa.backend.analysis.ExcludedCandidate
import squad.nononotaa.backend.analysis.ExclusionClassifier
import squad.nononotaa.backend.analysis.ExclusionResult
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.MentionSearch
import squad.nononotaa.backend.analysis.PreparedCandidates
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.ScoringResult
import squad.nononotaa.backend.analysis.SearchResult
import squad.nononotaa.backend.analysis.SourceReport
import squad.nononotaa.backend.config.ExclusionProperties
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.testsupport.StubChainServer

/** The real ExclusionClassifier bean (bundled lists + both on-chain sources) against a local stub of the services. */
class ExclusionOnchainCheckSpec extends BaseSpec {

    static final StubChainServer CHAIN = new StubChainServer()

    static final String KRAKEN = '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0'
    static final String ADDRESS = '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
    static final String OTHER = '0xd1220a0cf47c7b9be7a2e6ba89f429762e7b9adb'
    static final String RPC_SOURCE = 'Ethereum JSON-RPC eth_getCode (127.0.0.1): executable code present'
    static final String BLOCKCHAIR_SOURCE = 'Blockchair Ethereum address dashboard (127.0.0.1): executable code present'

    @DynamicPropertySource
    static void chainProperties(DynamicPropertyRegistry registry) {
        registry.add('app.exclusion.onchain-check-enabled') { 'true' }
        registry.add('app.exclusion.eth-rpc-url') { CHAIN.url('/rpc') }
        registry.add('app.exclusion.blockchair-url') { CHAIN.url('') }
        registry.add('app.exclusion.timeout') { 'PT1S' }
    }

    @Autowired
    ExclusionClassifier classifier

    @Autowired
    ExclusionProperties properties

    def setup() {
        CHAIN.reset()
    }

    def cleanupSpec() {
        CHAIN.stop()
    }

    private static Candidate eth(String address, List<String> mentionIds = ['m1']) {
        new Candidate('ETH', address, mentionIds)
    }

    def "binds the on-chain check configuration"() {
        expect:
        properties.onchainCheckEnabled
        properties.ethRpcUrl == URI.create(CHAIN.url('/rpc'))
        properties.blockchairUrl == URI.create(CHAIN.url(''))
        properties.timeout == Duration.ofSeconds(1)
    }

    def "an address with code is a smart contract, decided by the RPC alone"() {
        given:
        CHAIN.rpcCode(ADDRESS, '0x6080604052')
        def candidate = eth(ADDRESS)

        when:
        def result = classifier.classify([candidate])

        then:
        result == new ExclusionResult([], [
            new ExcludedCandidate(candidate, ExclusionReason.SMART_CONTRACT, RPC_SOURCE)
        ], [])
        CHAIN.requests == ["rpc $ADDRESS".toString()]
    }

    def "an account without code and an EIP-7702 delegated account are accepted"() {
        given:
        CHAIN.rpcCode(ADDRESS, '0x')
        CHAIN.rpcCode(OTHER, '0xef0100' + 'ab' * 20)

        when:
        def result = classifier.classify([eth(ADDRESS), eth(OTHER)])

        then:
        result.accepted*.address == [ADDRESS, OTHER]
        result.excluded.isEmpty()
        result.unclassified.isEmpty()
    }

    def "Blockchair decides when the RPC #failure"() {
        given:
        setUpRpc.call(ADDRESS)
        CHAIN.blockchairType(ADDRESS, type)
        def candidate = eth(ADDRESS)

        when:
        def result = classifier.classify([candidate])

        then:
        CHAIN.requests == [
            "rpc $ADDRESS".toString(),
            "blockchair $ADDRESS".toString()
        ]
        result.accepted == (type == 'account' ? [candidate] : [])
        result.excluded == (type == 'contract' ? [
            new ExcludedCandidate(candidate, ExclusionReason.SMART_CONTRACT, BLOCKCHAIR_SOURCE)
        ] : [])

        where:
        failure                  | type       | setUpRpc
        'is down (HTTP 503)'     | 'account'  | { String address -> }
        'returns an error'       | 'contract' | { String address ->
            CHAIN.rpcError(address)
        }
        'answers after timeout'  | 'contract' | { String address ->
            CHAIN.rpcCode(address, '0x', 2500)
        }
    }

    def "an address stays unclassified when both services are unavailable"() {
        given:
        def candidate = eth(ADDRESS)

        when:
        def result = classifier.classify([candidate])

        then:
        result == new ExclusionResult([], [], [candidate])
        CHAIN.requests == [
            "rpc $ADDRESS".toString(),
            "blockchair $ADDRESS".toString()
        ]
    }

    def "listed and BTC addresses are never sent to an on-chain service"() {
        when:
        def result = classifier.classify([
            eth(KRAKEN.toUpperCase().replace('0X', '0x')),
            new Candidate('BTC', '1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s', ['m2']),
            new Candidate('BTC', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa', ['m3'])
        ])

        then:
        CHAIN.requests.isEmpty()
        result.excluded*.reason == [
            ExclusionReason.EXCHANGE,
            ExclusionReason.EXCHANGE
        ]
        result.excluded*.classificationSource == [
            ExclusionLabelDirectory.ETHERSCAN_SOURCE + ': Kraken 4 [kraken]',
            'Own label list (https://www.walletexplorer.com/address/1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s): Binance.com'
        ]
        result.accepted*.address == [
            '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
        ]
    }

    def "in an Analysis with the real classifier, Excluded Addresses never reach the scoring and the unchecked address is flagged"() {
        given:
        CHAIN.rpcCode(ADDRESS, '0x6080604052')
        CHAIN.rpcCode(OTHER, '0x')
        def unchecked = eth('0xdbf03b407c01e7cd3cbea99509d93f8dddc8c6fb', ['m3'])
        def accepted = eth(OTHER, ['m2'])
        def candidates = [
            eth(KRAKEN, ['m1']),
            eth(ADDRESS, ['m1', 'm2']),
            accepted,
            unchecked
        ]
        def mention = new FoundMention('m1', 'forum', 'https://example.test/m1', Instant.parse('2026-11-21T10:00:00Z'), 'ctx', [])
        MentionSearch search = Stub() {
            search(_) >> new SearchResult([mention], [
                new SourceReport('forum', SourceStatus.CHECKED, true, Instant.parse('2026-11-21T10:00:00Z'))
            ])
        }
        CandidatePreparation preparation = Stub() {
            prepare(_) >> new PreparedCandidates(candidates, [])
        }
        ConfidenceScoring scoring = Mock()
        AnalysisResult finished = null
        AnalysisRecorder recorder = Stub() {
            begin(_) >> 'a-1'
            finish('a-1', _) >> { id, result -> finished = result }
        }
        def orchestrator = new AnalysisOrchestrator(search, preparation, classifier, scoring, recorder)

        when:
        orchestrator.run(new AnalysisRequest('case-1', AnalysisTrigger.MANUAL, []))

        then:
        1 * scoring.score([accepted, unchecked]) >> { args ->
            new ScoringResult(args[0].collect {
                new ScoredAttribution(it, 60, [
                    new AppliedRule('forum-post', 60)
                ], ClassificationStatus.NOT_EXCLUDED)
            }, 'v1')
        }
        finished.status == AnalysisStatus.PARTIAL
        finished.excluded*.reason == [
            ExclusionReason.EXCHANGE,
            ExclusionReason.SMART_CONTRACT
        ]
        finished.excluded*.candidate*.mentionIds == [['m1'], ['m1', 'm2']]
        finished.attributions*.candidate == [accepted, unchecked]
        finished.attributions*.classificationStatus == [
            ClassificationStatus.NOT_EXCLUDED,
            ClassificationStatus.UNAVAILABLE
        ]
    }
}
