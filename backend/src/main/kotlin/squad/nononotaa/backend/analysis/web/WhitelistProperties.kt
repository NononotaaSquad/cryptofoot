package squad.nononotaa.backend.analysis.web

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URLEncoder
import java.time.Duration

@ConfigurationProperties("sources.web")
data class WhitelistProperties(
    val enabled: Boolean = false,
    val botUserAgent: String = "OLCYS-Bot/1.0 (KAS OSINT tool; hacknation2026)",
    val maxTextLength: Int = 500_000,
    val minDelayBetweenRequests: Duration = Duration.ofSeconds(3),
    val respectRobotsTxt: Boolean = true,
    val headless: Boolean = true,
    val whitelist: List<WhitelistEntry> = emptyList(),
)

data class WhitelistEntry(
    val name: String,
    val searchUrl: String,
    val enabled: Boolean = true,
    val resultSelector: String? = null,
) {
    fun urlFor(identifier: String): String {
        val encoded = URLEncoder.encode(identifier, Charsets.UTF_8)
            .replace("+", "%20")
            .replace("%40", "@")
            .replace("%2E", ".")
            .replace("%2D", "-")
            .replace("%5F", "_")
        return searchUrl.replace("{identifier}", encoded)
    }
}
