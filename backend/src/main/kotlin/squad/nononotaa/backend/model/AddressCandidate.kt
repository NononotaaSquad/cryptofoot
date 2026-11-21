package squad.nononotaa.backend.model

enum class Network { BITCOIN, ETHEREUM }

data class CanonicalAddress(
    val network: Network,
    val address: String,
)

data class MentionAddresses(
    val mentionId: String,
    val detectedAddresses: List<String>,
)

data class AddressCandidate(
    val address: CanonicalAddress,
    val mentionIds: List<String>,
)

enum class RejectionReason { UNKNOWN_FORMAT, INVALID_ADDRESS }

data class RejectedCandidate(
    val mentionId: String,
    val value: String,
    val network: Network?,
    val reason: RejectionReason,
)

data class AddressCandidates(
    val valid: List<AddressCandidate>,
    val rejected: List<RejectedCandidate>,
)
