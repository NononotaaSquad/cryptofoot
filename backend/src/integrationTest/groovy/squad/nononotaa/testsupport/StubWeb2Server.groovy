package squad.nononotaa.testsupport

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Local stand-in for GitHub, Reddit and Bitcointalk (controlled HTTP, no internet). Answers are keyed by request path
 * (query ignored, except a Bitcointalk request, whose path is "/index.php?..." in full); an unknown path gets HTTP 404.
 * Records every request as "METHOD path?query" together with its headers.
 */
class StubWeb2Server {

    static class Answer {
        int status = 200
        String body = ''
        Map<String, String> headers = [:]
        long delayMillis
    }

    static class Request {
        String line
        Map<String, String> headers
    }

    final List<Request> requests = new CopyOnWriteArrayList<>()
    private final Map<String, Answer> answers = [:].asSynchronized()
    private final HttpServer server
    private final ExecutorService executor = Executors.newCachedThreadPool()

    StubWeb2Server() {
        server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        server.executor = executor
        server.createContext('/') { HttpExchange exchange ->
            def uri = exchange.requestURI
            def fullPath = uri.rawPath + (uri.rawQuery ? '?' + uri.rawQuery : '')
            requests << new Request(
                    line: "${exchange.requestMethod} ${fullPath}".toString(),
                    headers: exchange.requestHeaders.collectEntries { k, v ->
                        [(k.toLowerCase()): v.join(',')]
                    })
            def answer = answers[fullPath] ?: answers[uri.rawPath] ?: new Answer(status: 404, body: 'not found')
            if (answer.delayMillis> 0) {
                Thread.sleep(answer.delayMillis)
            }
            answer.headers.each { k, v -> exchange.responseHeaders.add(k, v) }
            def bytes = answer.body.getBytes(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(answer.status, bytes.length == 0 ? -1 : bytes.length)
            exchange.responseBody.withCloseable {
                if (bytes.length> 0) it.write(bytes)
            }
        }
        server.start()
    }

    String url() {
        "http://127.0.0.1:${server.address.port}"
    }

    void answer(String path, String body, int status = 200, Map<String, String> headers = [:], long delayMillis = 0) {
        answers[path] = new Answer(status: status, body: body, headers: headers, delayMillis: delayMillis)
    }

    void reset() {
        requests.clear()
        answers.clear()
    }

    List<String> requestLines() {
        requests*.line
    }

    void stop() {
        server.stop(0)
        executor.shutdownNow()
    }
}
