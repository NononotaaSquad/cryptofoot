package squad.nononotaa.backend.config

import java.time.Duration
import spock.lang.Specification

/** The defaults documented in decisions D22-003 and D22-005. */
class ExclusionPropertiesSpec extends Specification {

    def "the on-chain check is on by default against the keyless public services with a 5 second timeout"() {
        when:
        def properties = new ExclusionProperties()

        then:
        properties.onchainCheckEnabled
        properties.ethRpcUrl == URI.create('https://ethereum-rpc.publicnode.com')
        properties.blockchairUrl == URI.create('https://api.blockchair.com')
        properties.timeout == Duration.ofSeconds(5)
    }
}
