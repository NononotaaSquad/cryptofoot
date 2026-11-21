package squad.nononotaa.backend.service

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.KnownExcludedAddress
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.util.logger

/**
 * Our own versioned list of Excluded Addresses (ADR 0003), loaded once from the classpath.
 * No network calls: the brief forbids sending data to unapproved external services.
 * Every entry is validated like any candidate, so a typo in the list fails application startup.
 */
@Component
class OwnListClassificationSource(
    addressValidator: AddressValidator,
) : AddressClassificationSource {
    override val name = "Own list v$LIST_VERSION"

    override val networks = Network.entries.toSet()

    private val entries: Map<CanonicalAddress, KnownExcludedAddress> = load(addressValidator)

    override fun findExcluded(addresses: Collection<CanonicalAddress>): Map<CanonicalAddress, KnownExcludedAddress> = addresses.mapNotNull { address -> entries[address]?.let { address to it } }.toMap()

    private fun load(addressValidator: AddressValidator): Map<CanonicalAddress, KnownExcludedAddress> {
        val lines =
            ClassPathResource(LIST_PATH).inputStream.bufferedReader().useLines { lines ->
                lines.withIndex().filter { (_, line) -> line.isNotBlank() && !line.trimStart().startsWith("#") }.toList()
            }
        val loaded =
            lines.associate { (index, line) ->
                val columns = line.split(",", limit = 4).map { it.trim() }
                check(columns.size == 4) { "$LIST_PATH:${index + 1}: expected network,address,reason,label" }
                val (network, address, reason, label) = columns
                val validation = addressValidator.validate(address, Network.valueOf(network))
                check(validation is AddressValidation.Valid) { "$LIST_PATH:${index + 1}: invalid address ($validation)" }
                validation.canonicalAddress to KnownExcludedAddress(ExclusionReason.valueOf(reason), label)
            }
        check(loaded.size == lines.size) { "$LIST_PATH: duplicate addresses" }
        logger.info("Loaded {} Excluded Addresses from {} ({})", loaded.size, LIST_PATH, name)
        return loaded
    }

    companion object {
        const val LIST_VERSION = 1
        const val LIST_PATH = "classification/excluded-addresses-v$LIST_VERSION.csv"
        private val logger = logger()
    }
}
