package squad.nononotaa.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import squad.nononotaa.backend.analysis.ExclusionClassifier
import squad.nononotaa.backend.service.BlockchairContractCodeSource
import squad.nononotaa.backend.service.EthRpcContractCodeSource
import squad.nononotaa.backend.service.ExclusionClassifierService
import squad.nononotaa.backend.service.ExclusionLabelDirectory
import squad.nononotaa.backend.util.logger
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.time.Duration

/**
 * Keyless public services used for the ETH bytecode check (decisions D22-003, D22-005). Switch off with
 * `app.exclusion.onchain-check-enabled=false` if the mentors do not approve them: ETH addresses not on a label list
 * then stay unclassified, and nothing leaves the application.
 */
@ConfigurationProperties("app.exclusion")
data class ExclusionProperties(
    val onchainCheckEnabled: Boolean = true,
    val ethRpcUrl: URI = URI.create("https://ethereum-rpc.publicnode.com"),
    val blockchairUrl: URI = URI.create("https://api.blockchair.com"),
    val timeout: Duration = Duration.ofSeconds(5),
)

@Configuration
@EnableConfigurationProperties(ExclusionProperties::class)
class ExclusionConfig {
    @Bean
    fun exclusionLabelDirectory(mapper: ObjectMapper): ExclusionLabelDirectory {
        val directory =
            ExclusionLabelDirectory.load(
                mapper,
                ClassPathResource("exclusion-labels/etherscan-labels.json").inputStream,
                ClassPathResource("exclusion-labels/own-labels.json").inputStream,
            )
        logger.info("Loaded {} Excluded Address labels", directory.size)
        return directory
    }

    @Bean
    fun exclusionClassifier(
        labels: ExclusionLabelDirectory,
        properties: ExclusionProperties,
        mapper: ObjectMapper,
    ): ExclusionClassifier {
        val sources =
            if (properties.onchainCheckEnabled) {
                val http = HttpClient.newBuilder().connectTimeout(properties.timeout).build()
                listOf(
                    EthRpcContractCodeSource(properties.ethRpcUrl, properties.timeout, mapper, http),
                    BlockchairContractCodeSource(properties.blockchairUrl, properties.timeout, mapper, http),
                )
            } else {
                emptyList()
            }
        logger.info("Excluded Address on-chain check sources: {}", sources.map { it.sourceName }.ifEmpty { "disabled" })
        return ExclusionClassifierService(labels, sources)
    }

    companion object {
        private val logger = logger()
    }
}
