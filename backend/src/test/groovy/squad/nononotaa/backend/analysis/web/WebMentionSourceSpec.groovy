package squad.nononotaa.backend.analysis.web

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import spock.lang.Specification

import java.time.Duration

class WebMentionSourceSpec extends Specification {

    def "returns RawMention with url and content for each identifier when page loads"() {
        given:
        def entry = new WhitelistEntry("test-source", "https://example.com/search?q={identifier}", true, null)
        def props = new WhitelistProperties(true, "OLCYS-Bot/1.0", 500_000, Duration.ZERO, false, true, [entry])
        def mockPage = Mock(Page) {
            navigate(_, _) >> null
            content() >> "<html><body>zenek_fx BTC 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa</body></html>"
        }
        def mockContext = Mock(BrowserContext) { newPage() >> mockPage }
        def mockBrowser = Mock(Browser) { newContext(_) >> mockContext }
        def factory = Mock(PlaywrightFactory) { browser() >> mockBrowser }
        def source = new WebMentionSource(props, factory, Mock(RobotsChecker))
        def identifiers = [new CaseIdentifier(IdentifierType.NICKNAME, "zenek_fx")]

        when:
        def mentions = source.findMentions(identifiers)

        then:
        mentions.size() == 1
        mentions[0].url == "https://example.com/search?q=zenek_fx"
        mentions[0].content.contains("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")
        mentions[0].collectedAt != null
        source.sourceName == "web"
        !source.synthetic
    }

    def "skips url when robots.txt disallows it"() {
        given:
        def entry = new WhitelistEntry("blocked", "https://example.com/search?q={identifier}", true, null)
        def props = new WhitelistProperties(true, "OLCYS-Bot/1.0", 500_000, Duration.ZERO, true, true, [entry])
        def robots = Mock(RobotsChecker) { canFetch(_) >> false }
        def factory = Mock(PlaywrightFactory)
        def source = new WebMentionSource(props, factory, robots)
        def identifiers = [new CaseIdentifier(IdentifierType.NICKNAME, "zenek_fx")]

        when:
        def mentions = source.findMentions(identifiers)

        then:
        mentions.isEmpty()
        0 * factory.browser()
    }

    def "returns empty list when page fails to load (blocked/captcha)"() {
        given:
        def entry = new WhitelistEntry("captcha", "https://example.com/search?q={identifier}", true, null)
        def props = new WhitelistProperties(true, "OLCYS-Bot/1.0", 500_000, Duration.ZERO, false, true, [entry])
        def mockPage = Mock(Page) {
            navigate(_, _) >> null
            content() >> { throw new RuntimeException("CAPTCHA detected") }
        }
        def mockContext = Mock(BrowserContext) { newPage() >> mockPage }
        def mockBrowser = Mock(Browser) { newContext(_) >> mockContext }
        def factory = Mock(PlaywrightFactory) { browser() >> mockBrowser }
        def source = new WebMentionSource(props, factory, Mock(RobotsChecker))
        def identifiers = [new CaseIdentifier(IdentifierType.NICKNAME, "zenek_fx")]

        when:
        def mentions = source.findMentions(identifiers)

        then:
        mentions.isEmpty()
    }

    def "truncates content to maxTextLength"() {
        given:
        def longText = "x" * 600_000
        def entry = new WhitelistEntry("big", "https://example.com/search?q={identifier}", true, null)
        def props = new WhitelistProperties(true, "OLCYS-Bot/1.0", 500_000, Duration.ZERO, false, true, [entry])
        def mockPage = Mock(Page) {
            navigate(_, _) >> null
            content() >> longText
        }
        def mockContext = Mock(BrowserContext) { newPage() >> mockPage }
        def mockBrowser = Mock(Browser) { newContext(_) >> mockContext }
        def factory = Mock(PlaywrightFactory) { browser() >> mockBrowser }
        def source = new WebMentionSource(props, factory, Mock(RobotsChecker))
        def identifiers = [new CaseIdentifier(IdentifierType.NICKNAME, "zenek_fx")]

        when:
        def mentions = source.findMentions(identifiers)

        then:
        mentions.size() == 1
        mentions[0].content.length() == 500_000
    }

    def "returns empty list when no whitelist entries are enabled"() {
        given:
        def entry = new WhitelistEntry("disabled", "https://example.com/search?q={identifier}", false, null)
        def props = new WhitelistProperties(true, "OLCYS-Bot/1.0", 500_000, Duration.ZERO, false, true, [entry])
        def factory = Mock(PlaywrightFactory)
        def source = new WebMentionSource(props, factory, Mock(RobotsChecker))

        when:
        def mentions = source.findMentions([new CaseIdentifier(IdentifierType.NICKNAME, "zenek_fx")])

        then:
        mentions.isEmpty()
        0 * factory.browser()
    }
}
