package squad.nononotaa.backend.analysis.synthetic

import squad.nononotaa.backend.model.CashOutSignalKind
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.TransferDirection

data class SyntheticActivityFixture(
    val version: String,
    val synthetic: Boolean,
    val addresses: List<FixtureAddressActivity>,
)

data class FixtureAddressActivity(
    val network: Network,
    val address: String,
    val transfers: List<FixtureTransfer>,
)

data class FixtureTransfer(
    val transactionId: String,
    val occurredAt: String,
    val direction: TransferDirection,
    val amount: String,
    val unit: String,
    val tokenContract: String? = null,
    val counterparty: String? = null,
    val counterpartyLabel: FixtureCounterpartyLabel? = null,
)

data class FixtureCounterpartyLabel(
    val kind: CashOutSignalKind,
    val name: String,
)

data class SyntheticFixture(
    val version: String,
    val synthetic: Boolean,
    val person: FixturePerson,
    val mentions: List<FixtureMention>,
    // fictional demo scenarios: what to enter and what to expect (demo-scenarios.md)
    val scenarios: List<FixtureScenario> = emptyList(),
)

data class FixtureScenario(
    val id: String,
    val category: String,
    val title: String,
    val identifiers: List<FixtureIdentifier>,
    val expected: String,
)

data class FixturePerson(
    val firstName: String,
    val lastName: String,
    val identifiers: List<FixtureIdentifier>,
)

data class FixtureIdentifier(
    val type: String,
    val value: String,
)

data class FixtureMention(
    val url: String,
    val collectedAt: String,
    val content: String,
)
