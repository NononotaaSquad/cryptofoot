package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.ExclusionReason.SMART_CONTRACT
import static squad.nononotaa.backend.model.Network.BTC
import static squad.nononotaa.backend.model.Network.ETH

import com.sun.net.httpserver.HttpServer
import java.time.Duration
import spock.lang.Specification
import squad.nononotaa.backend.config.BlockchairProperties
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.KnownExcludedAddress

class BlockchairContractSourceSpec extends Specification {

    static final CONTRACT = new CanonicalAddress(ETH, '0xdac17f958d2ee523a2206206994597c13d831ec7')
    static final ACCOUNT = new CanonicalAddress(ETH, '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed')

    /** address -> [status, body]; requests are recorded so the spec can assert what left the application */
    Map<String, List> responses = [:]
    List<String> requests = []

    HttpServer server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0).tap {
        createContext('/ethereum/dashboards/address/') { exchange ->
            requests << exchange.requestURI.toString()
            def address = exchange.requestURI.path.tokenize('/').last()
            def (status, body) = responses.getOrDefault(address, [500, '{}'])
            def bytes = body.bytes
            exchange.sendResponseHeaders(status, bytes.length)
            exchange.responseBody.withCloseable { it.write(bytes) }
        }
        start()
    }

    def cleanup() {
        try {
            server.stop(0)
        } catch (IllegalStateException ignored) {
            // already stopped by the spec
        }
    }

    def source(String apiKey = null) {
        new BlockchairContractSource(new BlockchairProperties(true, "http://127.0.0.1:$server.address.port", apiKey, Duration.ofSeconds(2)))
    }

    static String dashboard(String address, String type) {
        """{"data":{"$address":{"address":{"type":"$type","contract_code_hex":null}}},"context":{"code":200}}"""
    }

    def "an address with contract code is a smart contract; a plain account is not excluded"() {
        given:
        responses[CONTRACT.address] = [
            200,
            dashboard(CONTRACT.address, 'contract')
        ]
        responses[ACCOUNT.address] = [
            200,
            dashboard(ACCOUNT.address, 'account')
        ]

        expect:
        source().findExcluded([CONTRACT, ACCOUNT]) == [(CONTRACT): new KnownExcludedAddress(SMART_CONTRACT, 'contract code present')]
        source().name == 'Blockchair contract check'
        source().networks == [ETH] as Set
    }

    def "only Ethereum addresses are sent, and the API key is passed when configured"() {
        given:
        responses[ACCOUNT.address] = [
            200,
            dashboard(ACCOUNT.address, 'account')
        ]

        when:
        source('secret').findExcluded([
            ACCOUNT,
            new CanonicalAddress(BTC, 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4')
        ])

        then:
        requests == [
            "/ethereum/dashboards/address/$ACCOUNT.address?key=secret"
        ]
    }

    def "an error, rate limit or unexpected body makes the source unavailable instead of guessing an account"() {
        given:
        responses[ACCOUNT.address] = [status, body]

        when:
        source().findExcluded([ACCOUNT])

        then:
        thrown(ClassificationUnavailableException)

        where:
        status | body
        429    | '{"context":{"code":429,"error":"Limit exceeded"}}'
        430    | '{}'
        500    | '{}'
        200    | 'not json'
        200    | '{"data":{},"context":{"code":200}}'
        200    | '{"data":{"x":{"address":{"type":null}}}}'
    }

    def "an unreachable API makes the source unavailable"() {
        given:
        server.stop(0)

        when:
        source().findExcluded([ACCOUNT])

        then:
        thrown(ClassificationUnavailableException)
    }

    def "together with the own list, a contract the list does not know becomes an Excluded Address"() {
        given:
        def unknownContract = new CanonicalAddress(ETH, '0x00000000219ab540356cbb839cbe05303d7705fa')
        responses[unknownContract.address] = [
            200,
            dashboard(unknownContract.address, 'contract')
        ]
        def candidate = new squad.nononotaa.backend.model.AddressCandidate(unknownContract, ['m1'])

        when:
        def result = new AddressExclusionService([
            new OwnListClassificationSource(new AddressValidator()),
            source()
        ]).classify([candidate])

        then:
        result.excluded*.reason == [SMART_CONTRACT]
        result.excluded*.classificationSource == [
            'Blockchair contract check: contract code present'
        ]
        result.checkedSources == [
            'Own list v1',
            'Blockchair contract check'
        ]
    }
}
