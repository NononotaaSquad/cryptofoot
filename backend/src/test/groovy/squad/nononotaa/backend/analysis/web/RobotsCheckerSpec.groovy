package squad.nononotaa.backend.analysis.web

import com.sun.net.httpserver.HttpServer
import spock.lang.Specification

import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class RobotsCheckerSpec extends Specification {

    HttpServer server
    int port
    Map<String, String> robotsResponses = new ConcurrentHashMap<>()
    Map<String, Integer> requestCounts = new ConcurrentHashMap<>()

    def setup() {
        server = HttpServer.create(new InetSocketAddress(0), 0)
        port = server.address.port
        server.createContext("/robots.txt") { exchange ->
            def host = exchange.requestHeaders.getFirst("Host")
            requestCounts.merge(host, 1, Integer::sum)
            def body = robotsResponses.getOrDefault(host, "")
            def bytes = body.getBytes("UTF-8")
            if (bytes.length == 0) {
                exchange.sendResponseHeaders(404, -1)
            } else {
                exchange.responseHeaders.set("Content-Type", "text/plain")
                exchange.sendResponseHeaders(200, bytes.length)
                exchange.responseBody.write(bytes)
            }
            exchange.close()
        }
        server.start()
    }

    def cleanup() {
        server?.stop(0)
    }

    def "allows path when robots.txt explicitly allows it"() {
        given:
        robotsResponses["localhost:$port"] = "User-agent: *\nAllow: /search\nDisallow: /private\n"
        def checker = new RobotsChecker()

        expect:
        checker.canFetch("http://localhost:$port/search?q=test")
    }

    def "blocks path when robots.txt disallows it"() {
        given:
        robotsResponses["localhost:$port"] = "User-agent: *\nDisallow: /search\n"
        def checker = new RobotsChecker()

        expect:
        !checker.canFetch("http://localhost:$port/search?q=test")
    }

    def "allows everything when robots.txt is missing (404)"() {
        given:
        def checker = new RobotsChecker()

        expect:
        checker.canFetch("http://localhost:$port/anything")
    }

    def "allows everything when robots.txt is empty"() {
        given:
        robotsResponses["localhost:$port"] = ""
        def checker = new RobotsChecker()

        expect:
        checker.canFetch("http://localhost:$port/anything")
    }

    def "caches robots.txt per domain - second call does not fetch again"() {
        given:
        robotsResponses["localhost:$port"] = "User-agent: *\nDisallow: /blocked\n"
        def checker = new RobotsChecker()

        when:
        checker.canFetch("http://localhost:$port/page1")
        checker.canFetch("http://localhost:$port/page2")

        then:
        requestCounts.values().sum() == 1
    }

    def "blocks all paths when robots.txt has Disallow: /"() {
        given:
        robotsResponses["localhost:$port"] = "User-agent: *\nDisallow: /\n"
        def checker = new RobotsChecker()

        expect:
        !checker.canFetch("http://localhost:$port/anything")
    }

    def "respects our user-agent OLCYS-Bot when present in robots.txt"() {
        given:
        robotsResponses["localhost:$port"] = "User-agent: OLCYS-Bot\nDisallow: /search\n\nUser-agent: *\nAllow: /\n"
        def checker = new RobotsChecker("OLCYS-Bot/1.0")

        expect:
        !checker.canFetch("http://localhost:$port/search?q=test")
        checker.canFetch("http://localhost:$port/profile")
    }
}
