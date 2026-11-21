package squad.nononotaa.backend.service

import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Requires
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.ExclusionClassifier
import squad.nononotaa.backend.model.ExclusionReason

class ExclusionClassifierWiringSpec extends BaseSpec {

    @Autowired
    ExclusionClassifier classifier

    def "the application classifies listed addresses from the bundled label lists without an on-chain call"() {
        given:
        def kraken = new Candidate('ETH', '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0', ['m1'])
        def tornado = new Candidate('ETH', '0xd90e2f925da726b50c4ed8d0fb90ad053324f31b', ['m2'])
        def donation = new Candidate('ETH', '0x165cd37b4c644c2921454429e7f9358d18a45e14', ['m3'])
        def person = new Candidate('BTC', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa', ['m4'])

        when:
        def result = classifier.classify([
            kraken,
            tornado,
            donation,
            person
        ])

        then:
        result.excluded*.reason == [
            ExclusionReason.EXCHANGE,
            ExclusionReason.MIXER,
            ExclusionReason.FUNDRAISER
        ]
        result.excluded.every { it.classificationSource }
        result.accepted == [person]
        result.unclassified.isEmpty()
    }

    // Calls the real keyless services; run with EXCLUSION_LIVE_CHECK=true (decision D22-003).
    @Requires({ env.EXCLUSION_LIVE_CHECK == 'true' })
    def "live: a DEX router is a smart contract and an EIP-7702 delegated account is not"() {
        given:
        def router = new Candidate('ETH', '0x7a250d5630b4cf539739df2c5dacb4c659f2488d', ['m1'])
        def delegatedAccount = new Candidate('ETH', '0xd8da6bf26964af9d7eed9e03e53415d37aa96045', ['m2'])

        when:
        def result = classifier.classify([router, delegatedAccount])

        then:
        result.excluded*.candidate == [router]
        result.excluded*.reason == [
            ExclusionReason.SMART_CONTRACT
        ]
        result.accepted == [delegatedAccount]
    }
}
