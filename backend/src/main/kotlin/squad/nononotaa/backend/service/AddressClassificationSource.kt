package squad.nononotaa.backend.service

import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.KnownExcludedAddress
import squad.nononotaa.backend.model.Network

/**
 * One source of Excluded Address classifications (own list now; approved external services later, ADR 0003).
 * A source that cannot answer throws [ClassificationUnavailableException]; it never returns an empty result instead.
 */
interface AddressClassificationSource {
    /** Stable, versioned name stored as the classification source, e.g. "Own list v1". */
    val name: String

    /** Networks this source can classify; addresses of other networks are not sent to it. */
    val networks: Set<Network>

    /** Returns only the given addresses this source lists as excluded. */
    fun findExcluded(addresses: Collection<CanonicalAddress>): Map<CanonicalAddress, KnownExcludedAddress>
}

class ClassificationUnavailableException(
    sourceName: String,
    cause: Throwable? = null,
) : RuntimeException("Classification source unavailable: $sourceName", cause)
