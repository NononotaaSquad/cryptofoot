package squad.nononotaa.backend.model

data class RawAddressCandidate(
    val mentionId: String,
    val rawAddress: String,
    val networkHint: Network? = null,
)

data class CanonicalAddress(
    val network: Network,
    val address: String,
)

sealed interface AddressValidation {
    data class Valid(
        val canonicalAddress: CanonicalAddress,
    ) : AddressValidation

    data class Invalid(
        val failure: AddressValidationFailure,
    ) : AddressValidation
}

data class AddressCandidate(
    val canonicalAddress: CanonicalAddress,
    val mentionIds: List<String>,
)

data class RejectedAddressCandidate(
    val mentionId: String,
    val rawAddress: String,
    val failure: AddressValidationFailure,
)

// TODO E3.1: confirm this input shape with the owner of the Confidence Level algorithm (ADR 0002)
data class AddressCandidates(
    val candidates: List<AddressCandidate>,
    val rejected: List<RejectedAddressCandidate>,
)
