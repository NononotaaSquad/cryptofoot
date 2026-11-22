package squad.nononotaa.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import squad.nononotaa.backend.analysis.AddressActivityCollector
import squad.nononotaa.backend.analysis.synthetic.SyntheticActivitySource
import squad.nononotaa.backend.service.AddressActivityEvaluator
import squad.nononotaa.backend.service.AddressActivityService
import squad.nononotaa.backend.service.BlockscoutActivitySource
import squad.nononotaa.backend.service.ExclusionLabelDirectory
import squad.nononotaa.backend.service.MempoolActivitySource
import squad.nononotaa.backend.util.logger
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.time.Duration

// live explorers are off by default: an Attribution address is an Analysis result (brief 5.7, D23-003)
@ConfigurationProperties("app.address-activity")
data class AddressActivityProperties(
    val liveEnabled: Boolean = false,
    val mempoolUrl: URI = URI.create("https://mempool.space"),
    val blockscoutUrl: URI = URI.create("https://eth.blockscout.com"),
    val timeout: Duration = Duration.ofSeconds(5),
    val stablecoinSwapThreshold: BigDecimal = BigDecimal("1000"),
)

@Configuration
@EnableConfigurationProperties(AddressActivityProperties::class)
class AddressActivityConfig {
    @Bean
    fun addressActivityCollector(
        properties: AddressActivityProperties,
        labels: ExclusionLabelDirectory,
        mapper: ObjectMapper,
        @Value("\${sources.synthetic.enabled:true}") syntheticEnabled: Boolean,
    ): AddressActivityCollector {
        val synthetic = if (syntheticEnabled) listOf(SyntheticActivitySource.load(mapper)) else emptyList()
        val live =
            if (properties.liveEnabled) {
                val http = HttpClient.newBuilder().connectTimeout(properties.timeout).build()
                listOf(
                    MempoolActivitySource(properties.mempoolUrl, properties.timeout, mapper, http),
                    BlockscoutActivitySource(properties.blockscoutUrl, properties.timeout, mapper, http),
                )
            } else {
                emptyList()
            }
        val sources = synthetic + live
        logger.info(
            "Address Activity sources: {}; stablecoin swap threshold {}",
            sources.map { it.sourceName }.ifEmpty { "none" },
            properties.stablecoinSwapThreshold,
        )
        return AddressActivityService(sources, AddressActivityEvaluator(labels, properties.stablecoinSwapThreshold))
    }

    companion object {
        private val logger = logger()
    }
}
