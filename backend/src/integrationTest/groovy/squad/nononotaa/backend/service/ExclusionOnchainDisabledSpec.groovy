package squad.nononotaa.backend.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.ExclusionClassifier
import squad.nononotaa.backend.config.ExclusionProperties
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.testsupport.StubChainServer

/** The documented kill switch (D22-005): the environment variable turns the on-chain check off end to end. */
@TestPropertySource(properties = 'EXCLUSION_ONCHAIN_CHECK_ENABLED=false')
class ExclusionOnchainDisabledSpec extends BaseSpec {

    static final StubChainServer CHAIN = new StubChainServer()

    @DynamicPropertySource
    static void chainProperties(DynamicPropertyRegistry registry) {
        registry.add('app.exclusion.eth-rpc-url') { CHAIN.url('/rpc') }
        registry.add('app.exclusion.blockchair-url') { CHAIN.url('') }
    }

    @Autowired
    ExclusionClassifier classifier

    @Autowired
    ExclusionProperties properties

    def cleanupSpec() {
        CHAIN.stop()
    }

    def "with the switch off nothing is sent out, listed addresses are still excluded and the rest of ETH is unclassified"() {
        given:
        CHAIN.rpcCode('0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed', '0x6080604052')
        def unlisted = new Candidate('ETH', '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed', ['m1'])
        def kraken = new Candidate('ETH', '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0', ['m2'])
        def btc = new Candidate('BTC', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa', ['m3'])

        when:
        def result = classifier.classify([unlisted, kraken, btc])

        then:
        !properties.onchainCheckEnabled
        CHAIN.requests.isEmpty()
        result.unclassified == [unlisted]
        result.excluded*.candidate == [kraken]
        result.excluded*.reason == [ExclusionReason.EXCHANGE]
        result.accepted == [btc]
    }
}
