package squad.nononotaa.backend.model

/** What one classification source knows about an address it lists (e.g. reason EXCHANGE, label "Binance 14"). */
data class KnownExcludedAddress(
    val reason: ExclusionReason,
    val label: String,
)

/** A candidate that must not be connected with a person. It never reaches the Confidence Level algorithm. */
data class ExcludedAddress(
    val candidate: AddressCandidate,
    val reason: ExclusionReason,
    val classificationSource: String,
)

/**
 * Result of classifying address candidates (ADR 0003).
 * - [accepted]: every applicable source answered and none lists the address; it goes to the Confidence Level algorithm.
 * - [excluded]: at least one source lists the address; it never becomes an Attribution.
 * - [unavailable]: not listed by any source that answered, but at least one applicable source could not answer;
 *   this is not a confirmed exclusion and not a confirmed "not excluded".
 */
data class AddressClassifications(
    val accepted: List<AddressCandidate>,
    val excluded: List<ExcludedAddress>,
    val unavailable: List<AddressCandidate>,
    val checkedSources: List<String>,
    val unavailableSources: List<String>,
)
