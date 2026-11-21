package squad.nononotaa.backend.analysis.web

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.MentionSource
import squad.nononotaa.backend.analysis.RawMention
import squad.nononotaa.backend.util.logger
import java.time.Duration
import java.time.Instant

@Component
@ConditionalOnProperty(name = ["sources.web.enabled"], havingValue = "true")
class WebMentionSource(
    private val props: WhitelistProperties,
    private val playwrightFactory: PlaywrightFactory,
    private val robotsChecker: RobotsChecker,
) : MentionSource {
    override val sourceName: String = "web"

    override val synthetic: Boolean = false

    private var lastRequestAt: Instant? = null

    override fun findMentions(identifiers: List<CaseIdentifier>): List<RawMention> {
        val mentions = mutableListOf<RawMention>()
        val activeEntries = props.whitelist.filter { it.enabled }
        if (activeEntries.isEmpty()) return emptyList()
        for (entry in activeEntries) {
            for (identifier in identifiers) {
                if (identifier.value.isBlank()) continue
                val url = entry.urlFor(identifier.value.trim())
                if (props.respectRobotsTxt && !robotsChecker.canFetch(url)) {
                    logger.info("Skipping entry={} url={} blocked by robots.txt", entry.name, url)
                    continue
                }
                throttle()
                try {
                    val text = fetchPageText(playwrightFactory.browser(), url)
                    if (text.isNotBlank()) {
                        mentions += RawMention(url, Instant.now(), text)
                        logger.info("Mention found entry={} url={} length={}", entry.name, url, text.length)
                    }
                } catch (e: Exception) {
                    logger.warn("Page fetch failed entry={} url={} error={}", entry.name, url, e.message)
                }
            }
        }
        return mentions
    }

    private fun throttle() {
        val last = lastRequestAt
        if (last != null) {
            val elapsed = Duration.between(last, Instant.now())
            val remaining = props.minDelayBetweenRequests.minus(elapsed)
            if (!remaining.isNegative && !remaining.isZero) {
                Thread.sleep(remaining.toMillis())
            }
        }
        lastRequestAt = Instant.now()
    }

    private fun fetchPageText(
        browser: com.microsoft.playwright.Browser,
        url: String,
    ): String {
        browser.newContext(
            com.microsoft.playwright.Browser.NewContextOptions()
                .setUserAgent(props.botUserAgent),
        ).use { context ->
            context.newPage().use { page ->
                page.navigate(url, com.microsoft.playwright.Page.NavigateOptions().setTimeout(30_000.0))
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED)
                try {
                    page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE)
                } catch (e: Exception) {
                    logger.debug("networkidle timeout for url={}", url)
                }
                page.waitForTimeout(1500.0)
                val html = page.content()
                val text = htmlToText(html)
                return if (text.length > props.maxTextLength) text.take(props.maxTextLength) else text
            }
        }
    }

    private fun htmlToText(html: String): String {
        return html
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private val logger = logger()
    }
}
