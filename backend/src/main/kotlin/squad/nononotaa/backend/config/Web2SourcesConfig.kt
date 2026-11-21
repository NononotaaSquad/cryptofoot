package squad.nononotaa.backend.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import squad.nononotaa.backend.analysis.MentionSource
import squad.nononotaa.backend.analysis.web2.BitcointalkMentionSource
import squad.nononotaa.backend.analysis.web2.GitHubMentionSource
import squad.nononotaa.backend.util.logger
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.time.Clock
import java.time.Duration

/**
 * Real Web2 sources (task #61). Every source is off by default: enabling GitHub sends the investigated person's
 * login/nickname to the service, enabling Bitcointalk fetches the URL the Analyst gave.
 */
@ConfigurationProperties("sources")
data class Web2Properties(
    val github: GitHub = GitHub(),
    val bitcointalk: Bitcointalk = Bitcointalk(),
) {
    data class GitHub(
        val enabled: Boolean = false,
        val apiUrl: URI = URI.create("https://api.github.com"),
        val token: String = "",
        val timeout: Duration = Duration.ofSeconds(5),
        val maxBytes: Int = 262_144,
        val maxLogins: Int = 3,
    )

    data class Bitcointalk(
        val enabled: Boolean = false,
        val baseUrl: URI = URI.create("https://bitcointalk.org"),
        val userAgent: String = "cryptofoot-backend/0.0.1",
        val timeout: Duration = Duration.ofSeconds(5),
        val maxBytes: Int = 1_048_576,
        val maxResources: Int = 3,
        val maxPosts: Int = 20,
        val requestDelay: Duration = Duration.ofSeconds(2),
    )
}

@Configuration
@EnableConfigurationProperties(Web2Properties::class)
class Web2SourcesConfig {
    @Bean
    @ConditionalOnProperty("sources.github.enabled", havingValue = "true")
    fun githubMentionSource(
        properties: Web2Properties,
        mapper: ObjectMapper,
    ): MentionSource = GitHubMentionSource(properties.github, mapper, Clock.systemUTC()).also { logger.info("GitHub source enabled") }

    @Bean
    @ConditionalOnProperty("sources.bitcointalk.enabled", havingValue = "true")
    fun bitcointalkMentionSource(properties: Web2Properties): MentionSource = BitcointalkMentionSource(properties.bitcointalk, Clock.systemUTC()).also {
        logger.info("Bitcointalk source enabled")
    }

    companion object {
        private val logger = logger()
    }
}
