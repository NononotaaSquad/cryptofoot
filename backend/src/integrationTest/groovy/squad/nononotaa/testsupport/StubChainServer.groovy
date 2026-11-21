package squad.nononotaa.testsupport

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Local stand-in for the keyless on-chain services (Ethereum JSON-RPC at /rpc, Blockchair dashboards at /ethereum/...,
 * mempool.space and Blockscout explorer APIs at /api/...), so integration specs exercise the real wiring without the
 * internet. Records every request as "rpc 0x...", "blockchair 0x..." or "explorer /api/...?query". A request without
 * a prepared answer gets HTTP 503.
 */
class StubChainServer {

    static class Answer {
        int status
        String body
        long delayMillis
    }

    final List<String> requests = new CopyOnWriteArrayList<>()
    private final Map<String, Answer> rpcAnswers = [:].asSynchronized()
    private final Map<String, Answer> blockchairAnswers = [:].asSynchronized()
    private final Map<String, Answer> explorerAnswers = [:].asSynchronized()
    private final HttpServer server
    private final ExecutorService executor = Executors.newCachedThreadPool()

    StubChainServer() {
        server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        server.executor = executor
        server.createContext('/rpc') { HttpExchange exchange ->
            def address = (exchange.requestBody.getText('UTF-8') =~ /0x[0-9a-fA-F]{40}/).with {
                it.find() ? it.group() : '?'
            }
            requests << "rpc $address".toString()
            respond(exchange, rpcAnswers[address])
        }
        server.createContext('/ethereum/dashboards/address/') { HttpExchange exchange ->
            def address = exchange.requestURI.path.tokenize('/').last()
            requests << "blockchair $address".toString()
            respond(exchange, blockchairAnswers[address])
        }
        server.createContext('/api/') { HttpExchange exchange ->
            def query = exchange.requestURI.rawQuery
            def target = exchange.requestURI.path + (query ? "?$query" : '')
            requests << "explorer $target".toString()
            respond(exchange, explorerAnswers[target])
        }
        server.start()
    }

    String url(String path) {
        "http://127.0.0.1:${server.address.port}$path"
    }

    void reset() {
        requests.clear()
        rpcAnswers.clear()
        blockchairAnswers.clear()
        explorerAnswers.clear()
    }

    void stop() {
        server.stop(0)
        executor.shutdownNow()
    }

    void rpcCode(String address, String code, long delayMillis = 0) {
        rpcAnswers[address] = new Answer(status: 200, body: """{"jsonrpc":"2.0","id":1,"result":"$code"}""", delayMillis: delayMillis)
    }

    void rpcError(String address) {
        rpcAnswers[address] = new Answer(status: 200, body: '{"jsonrpc":"2.0","id":1,"error":{"code":-32005,"message":"limit"}}')
    }

    void blockchairType(String address, String type) {
        blockchairAnswers[address] = new Answer(status: 200,
        body: """{"data":{"$address":{"address":{"type":"$type"}}},"context":{"code":200}}""")
    }

    void explorer(String pathAndQuery, String body) {
        explorerAnswers[pathAndQuery] = new Answer(status: 200, body: body)
    }

    // an address with a complete, empty history: Address Activity is AVAILABLE with zero activity
    void mempoolQuiet(String address) {
        explorer("/api/address/$address", """{"address":"$address","chain_stats":{"funded_txo_count":0,"funded_txo_sum":0,
            "spent_txo_count":0,"spent_txo_sum":0,"tx_count":0}}""")
        explorer("/api/address/$address/txs", '[]')
    }

    void blockscoutQuiet(String address) {
        def base = "/api/v2/addresses/$address"
        explorer("$base/counters", '{"transactions_count":"0"}')
        explorer("$base/transactions", '{"items":[],"next_page_params":null}')
        explorer("$base/token-transfers?type=ERC-20", '{"items":[],"next_page_params":null}')
    }

    private static void respond(HttpExchange exchange, Answer answer) {
        def chosen = answer ?: new Answer(status: 503, body: 'unavailable')
        if (chosen.delayMillis> 0) {
            Thread.sleep(chosen.delayMillis)
        }
        def bytes = chosen.body.getBytes(StandardCharsets.UTF_8)
        exchange.sendResponseHeaders(chosen.status, bytes.length)
        exchange.responseBody.withCloseable { it.write(bytes) }
    }
}
