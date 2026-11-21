package squad.nononotaa.backend.analysis.web

import spock.lang.Specification

class WhitelistPropertiesSpec extends Specification {

    def "substitutes identifier placeholder in searchUrl"() {
        given:
        def entry = new WhitelistEntry("test", "https://example.com/search?q={identifier}&lang=en", true, null)

        expect:
        entry.urlFor("zenek_fx") == "https://example.com/search?q=zenek_fx&lang=en"
        entry.urlFor("zenon.fikcyjny@example.com") == "https://example.com/search?q=zenon.fikcyjny@example.com&lang=en"
    }

    def "urlFor encodes identifier for safe URL use"() {
        given:
        def entry = new WhitelistEntry("test", "https://example.com/search?q={identifier}", true, null)

        expect:
        entry.urlFor("imię nazwisko") == "https://example.com/search?q=imi%C4%99%20nazwisko"
    }

    def "whitelist entry defaults enabled to true and resultSelector to null"() {
        given:
        def entry = new WhitelistEntry("test", "https://example.com/search?q={identifier}", true, null)

        expect:
        entry.enabled
        entry.resultSelector == null
    }
}
