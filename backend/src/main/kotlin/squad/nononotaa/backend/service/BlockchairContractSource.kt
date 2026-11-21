package squad.nononotaa.backend.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import squad.nononotaa.backend.config.BlockchairProperties
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.KnownExcludedAddress
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.util.logger
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import java.net.http.HttpClient

/**
 * KROK 3.2 of the brief: an Ethereum address with executable code is a smart contract, not a private account (ADR 0004).
 * Asks Blockchair's address dashboard for `data.<address>.address.type` ("account" or "contract").
 * Any HTTP error, rate limit, timeout or unexpected body makes the source unavailable; it never guesses "account".
 */
@Component
@Order(2)
@ConditionalOnProperty(prefix = "classification.blockchair", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class BlockchairContractSource(
    private val properties: BlockchairProperties,
) : AddressClassificationSource {
    override val name = "Blockchair contract check"

    override val networks = setOf(Network.ETH)

    private val jsonMapper = JsonMapper.builder().build()

    private val restClient =
        RestClient
            .builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(
                JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(properties.timeout).build())
                    .apply { setReadTimeout(properties.timeout) },
            ).build()

    override fun findExcluded(addresses: Collection<CanonicalAddress>): Map<CanonicalAddress, KnownExcludedAddress> = addresses
        .filter { it.network == Network.ETH }
        .filter { isContract(it.address) }
        .associateWith { KnownExcludedAddress(ExclusionReason.SMART_CONTRACT, "contract code present") }

    private fun isContract(address: String): Boolean {
        val body =
            try {
                restClient
                    .get()
                    .uri { builder ->
                        builder.path("/ethereum/dashboards/address/{address}")
                        properties.apiKey?.takeIf { it.isNotBlank() }?.let { builder.queryParam("key", it) }
                        builder.build(address)
                    }.retrieve()
                    .body(String::class.java)
            } catch (e: RestClientException) {
                throw ClassificationUnavailableException(name, e)
            }
        val type =
            try {
                val root = jsonMapper.readTree(body.orEmpty())
                root.path("data").values().firstOrNull()?.path("address")?.path("type")
            } catch (e: JacksonException) {
                throw ClassificationUnavailableException(name, e)
            }
        return when (val value = type?.takeIf { it.isString }?.stringValue()) {
            "contract" -> true

            "account" -> false

            else -> {
                logger.warn("Blockchair returned no address type (type: {})", value)
                throw ClassificationUnavailableException(name)
            }
        }
    }

    companion object {
        private val logger = logger()
    }
}
