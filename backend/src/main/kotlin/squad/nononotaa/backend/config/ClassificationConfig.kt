package squad.nononotaa.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

/** External Excluded Address classification via Blockchair (ADR 0004). Switch off with `classification.blockchair.enabled=false`. */
@ConfigurationProperties("classification.blockchair")
data class BlockchairProperties(
    val enabled: Boolean = true,
    val baseUrl: String = "https://api.blockchair.com",
    val apiKey: String? = null,
    val timeout: Duration = Duration.ofSeconds(10),
)

@Configuration
@EnableConfigurationProperties(BlockchairProperties::class)
class ClassificationConfig
