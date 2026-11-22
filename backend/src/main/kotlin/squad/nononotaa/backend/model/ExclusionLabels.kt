package squad.nononotaa.backend.model

/** A known non-personal address from a label list; it makes the candidate an Excluded Address (decision D22-002). */
data class ExclusionLabel
@JvmOverloads
constructor(
    val reason: ExclusionReason,
    val name: String,
    val source: String,
    // raw dataset tags; an own-list entry has none (D23-010)
    val tags: List<String> = emptyList(),
)

/** Result of the bytecode check of an ETH address (decisions D22-003, D22-004). */
enum class ContractCodeCheck {
    /** No code, or only an EIP-7702 delegation designator: a private account. */
    ACCOUNT,

    /** Executable code: a smart contract, never a private account. */
    CONTRACT,

    /** The service did not answer usably; nothing is concluded. */
    UNAVAILABLE,
}
