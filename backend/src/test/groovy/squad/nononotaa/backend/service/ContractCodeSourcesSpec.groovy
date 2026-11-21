package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.ContractCodeCheck.ACCOUNT
import static squad.nononotaa.backend.model.ContractCodeCheck.CONTRACT
import static squad.nononotaa.backend.model.ContractCodeCheck.UNAVAILABLE

import com.sun.net.httpserver.HttpServer
import java.net.http.HttpClient
import java.nio.charset.StandardCharsets
import java.time.Duration
import spock.lang.Specification
import tools.jackson.module.kotlin.ExtensionsKt

class ContractCodeSourcesSpec extends Specification {

    static final String ADDRESS = '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'
    static final String DELEGATION = '0xef0100' + 'ab' * 20

    def mapper = ExtensionsKt.jacksonObjectMapper()
    def http = HttpClient.newHttpClient()
    def timeout = Duration.ofSeconds(2)

    int responseStatus = 200
    String responseBody = ''
    String lastRequest
    HttpServer server

    def setup() {
        server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        server.createContext('/') { exchange ->
            lastRequest = "${exchange.requestMethod} ${exchange.requestURI} ${exchange.requestBody.getText('UTF-8')}"
            def bytes = responseBody.getBytes(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(responseStatus, bytes.length == 0 ? -1 : bytes.length)
            exchange.responseBody.withCloseable { it.write(bytes) }
        }
        server.start()
    }

    def cleanup() {
        server.stop(0)
    }

    private URI baseUri() {
        URI.create("http://127.0.0.1:${server.address.port}")
    }

    def "the RPC source reads eth_getCode result #code as #expected"() {
        given:
        def source = new EthRpcContractCodeSource(baseUri(), timeout, mapper, http)
        responseBody = """{"jsonrpc":"2.0","id":1,"result":"$code"}"""

        expect:
        source.check(ADDRESS) == expected
        lastRequest.startsWith('POST /')
        lastRequest.contains('"method":"eth_getCode"')
        lastRequest.contains(ADDRESS)

        where:
        code                                   | expected
        '0x'                                   | ACCOUNT
        '0x6080604052600436'                   | CONTRACT
        DELEGATION                             | ACCOUNT
        '0x' + DELEGATION.substring(2).toUpperCase() | ACCOUNT
        DELEGATION + '00'                      | CONTRACT
    }

    def "the RPC source is unavailable on #description"() {
        given:
        def source = new EthRpcContractCodeSource(baseUri(), timeout, mapper, http)
        responseStatus = status
        responseBody = body

        expect:
        source.check(ADDRESS) == UNAVAILABLE

        where:
        description        | status | body
        'a JSON-RPC error' | 200    | '{"jsonrpc":"2.0","id":1,"error":{"code":-32005,"message":"rate limited"}}'
        'an HTTP error'    | 503    | 'busy'
        'a non-JSON body'  | 200    | '<html>'
        'a null result'    | 200    | '{"jsonrpc":"2.0","id":1,"result":null}'
    }

    def "a source that cannot be reached is unavailable"() {
        given:
        def unreachable = URI.create('http://127.0.0.1:1')

        expect:
        new EthRpcContractCodeSource(unreachable, timeout, mapper, http).check(ADDRESS) == UNAVAILABLE
        new BlockchairContractCodeSource(unreachable, timeout, mapper, http).check(ADDRESS) == UNAVAILABLE
    }

    def "the Blockchair source reads type #type with code #code as #expected"() {
        given:
        def source = new BlockchairContractCodeSource(baseUri(), timeout, mapper, http)
        def codeField = code == null ? '' : ",\"contract_code_hex\":\"$code\""
        responseBody = """{"data":{"$ADDRESS":{"address":{"type":"$type"$codeField}}},"context":{"code":200}}"""

        expect:
        source.check(ADDRESS) == expected
        lastRequest.startsWith("GET /ethereum/dashboards/address/$ADDRESS")

        where:
        type       | code                    | expected
        'account'  | null                    | ACCOUNT
        'contract' | null                    | CONTRACT
        'contract' | '6080604052'            | CONTRACT
        'contract' | DELEGATION.substring(2) | ACCOUNT
        'unknown'  | null                    | UNAVAILABLE
    }

    def "the Blockchair source is unavailable when it refuses the request"() {
        given:
        def source = new BlockchairContractCodeSource(baseUri(), timeout, mapper, http)
        responseBody = '{"data":null,"context":{"code":430,"error":"Your IP address is temporary blacklisted"}}'

        expect:
        source.check(ADDRESS) == UNAVAILABLE
    }
}
