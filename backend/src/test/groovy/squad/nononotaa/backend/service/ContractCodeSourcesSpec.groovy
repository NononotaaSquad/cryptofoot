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
    long responseDelayMillis = 0
    String lastRequest
    String lastBody
    Map<String, List<String>> lastHeaders
    int requestCount = 0
    HttpServer server

    def setup() {
        server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        server.createContext('/') { exchange ->
            requestCount++
            lastBody = exchange.requestBody.getText('UTF-8')
            lastRequest = "${exchange.requestMethod} ${exchange.requestURI} ${lastBody}"
            lastHeaders = exchange.requestHeaders.collectEntries { k, v ->
                [(k.toLowerCase()): v]
            }
            if (responseDelayMillis> 0) {
                Thread.sleep(responseDelayMillis)
            }
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

    private EthRpcContractCodeSource rpc(Duration requestTimeout = timeout) {
        new EthRpcContractCodeSource(baseUri(), requestTimeout, mapper, http)
    }

    private BlockchairContractCodeSource blockchair(Duration requestTimeout = timeout) {
        new BlockchairContractCodeSource(baseUri(), requestTimeout, mapper, http)
    }

    def "the RPC source reads eth_getCode result #code as #expected"() {
        given:
        responseBody = """{"jsonrpc":"2.0","id":1,"result":"$code"}"""

        expect:
        rpc().check(ADDRESS) == expected

        where:
        code                                         | expected
        '0x'                                         | ACCOUNT
        '0X'                                         | UNAVAILABLE
        '0x6080604052600436'                         | CONTRACT
        DELEGATION                                   | ACCOUNT
        '0x' + DELEGATION.substring(2).toUpperCase() | ACCOUNT
        DELEGATION + '00'                            | CONTRACT
        DELEGATION.substring(0, DELEGATION.length() - 2) | CONTRACT
        '0xef0200' + 'ab' * 20                       | CONTRACT
    }

    def "the RPC source sends only a JSON-RPC eth_getCode call for the address, with a generic user agent"() {
        given:
        responseBody = '{"jsonrpc":"2.0","id":1,"result":"0x"}'

        when:
        rpc().check(ADDRESS)

        then:
        requestCount == 1
        lastRequest.startsWith('POST / ')
        mapper.readValue(lastBody, Map) == [jsonrpc: '2.0', id: 1, method: 'eth_getCode', params: [ADDRESS, 'latest']]
        lastHeaders['content-type'] == ['application/json']
        lastHeaders['user-agent'] == ['osint-analysis-backend']
    }

    def "the RPC source is unavailable on #description"() {
        given:
        responseStatus = status
        responseBody = body

        expect:
        rpc().check(ADDRESS) == UNAVAILABLE

        where:
        description            | status | body
        'a JSON-RPC error'     | 200    | '{"jsonrpc":"2.0","id":1,"error":{"code":-32005,"message":"rate limited"}}'
        'an HTTP error'        | 503    | 'busy'
        'a rate limit'         | 429    | '{"jsonrpc":"2.0","id":1,"result":"0x"}'
        'a non-JSON body'      | 200    | '<html>'
        'an empty body'        | 200    | ''
        'a null result'        | 200    | '{"jsonrpc":"2.0","id":1,"result":null}'
        'a non-hex result'     | 200    | '{"jsonrpc":"2.0","id":1,"result":"latest"}'
        'a numeric result'     | 200    | '{"jsonrpc":"2.0","id":1,"result":0}'
        'a JSON array'         | 200    | '[]'
    }

    def "a source that answers too late is unavailable"() {
        given:
        responseDelayMillis = 1500
        responseBody = '{"jsonrpc":"2.0","id":1,"result":"0x6080"}'

        expect:
        rpc(Duration.ofMillis(200)).check(ADDRESS) == UNAVAILABLE
        blockchair(Duration.ofMillis(200)).check(ADDRESS) == UNAVAILABLE
    }

    def "a source that cannot be reached is unavailable"() {
        given:
        def unreachable = URI.create('http://127.0.0.1:1')

        expect:
        new EthRpcContractCodeSource(unreachable, timeout, mapper, http).check(ADDRESS) == UNAVAILABLE
        new BlockchairContractCodeSource(unreachable, timeout, mapper, http).check(ADDRESS) == UNAVAILABLE
    }

    def "an interrupted check is unavailable and keeps the interrupt flag"() {
        given:
        responseBody = '{"jsonrpc":"2.0","id":1,"result":"0x6080"}'

        when:
        Thread.currentThread().interrupt()
        def result = rpc().check(ADDRESS)
        def stillInterrupted = Thread.interrupted()

        then:
        result == UNAVAILABLE
        stillInterrupted
    }

    def "the sources name the host they asked"() {
        expect:
        rpc().sourceName == 'Ethereum JSON-RPC eth_getCode (127.0.0.1)'
        blockchair().sourceName == 'Blockchair Ethereum address dashboard (127.0.0.1)'
    }

    def "the Blockchair source reads type #type with code #code as #expected"() {
        given:
        def codeField = code == null ? '' : ",\"contract_code_hex\":$code"
        responseBody = """{"data":{"$ADDRESS":{"address":{"type":"$type"$codeField}}},"context":{"code":200}}"""

        expect:
        blockchair().check(ADDRESS) == expected

        where:
        type       | code                                 | expected
        'account'  | null                                 | ACCOUNT
        'contract' | null                                 | CONTRACT
        'contract' | 'null'                               | CONTRACT
        'contract' | '"6080604052"'                       | CONTRACT
        'contract' | "\"${DELEGATION.substring(2)}\""     | ACCOUNT
        'contract' | "\"$DELEGATION\""                    | ACCOUNT
        'unknown'  | null                                 | UNAVAILABLE
    }

    def "the Blockchair source asks the address dashboard with a GET and a generic user agent"() {
        given:
        responseBody = """{"data":{"$ADDRESS":{"address":{"type":"account"}}},"context":{"code":200}}"""

        when:
        blockchair().check(ADDRESS)

        then:
        requestCount == 1
        lastRequest.startsWith("GET /ethereum/dashboards/address/$ADDRESS ")
        lastBody == ''
        lastHeaders['user-agent'] == ['osint-analysis-backend']
    }

    def "the Blockchair source is unavailable on #description"() {
        given:
        responseStatus = status
        responseBody = body

        expect:
        blockchair().check(ADDRESS) == UNAVAILABLE

        where:
        description                  | status | body
        'a refusal in the body'      | 200    | '{"data":null,"context":{"code":430,"error":"Your IP address is temporary blacklisted"}}'
        'a refusal as HTTP status'   | 430    | '{"data":null,"context":{"code":430}}'
        'an empty data object'       | 200    | '{"data":{},"context":{"code":200}}'
        'no context code'            | 200    | """{"data":{"$ADDRESS":{"address":{"type":"account"}}}}"""
        'no type'                    | 200    | """{"data":{"$ADDRESS":{"address":{}}},"context":{"code":200}}"""
        'no address details'         | 200    | """{"data":{"$ADDRESS":{}},"context":{"code":200}}"""
        'a non-JSON body'            | 200    | '<html>'
    }
}
