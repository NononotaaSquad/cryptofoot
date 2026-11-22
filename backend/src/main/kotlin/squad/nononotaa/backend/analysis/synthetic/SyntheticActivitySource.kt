package squad.nononotaa.backend.analysis.synthetic

import org.springframework.core.io.ClassPathResource
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.CashOutSignalKind
import squad.nononotaa.backend.service.AddressActivitySource
import squad.nononotaa.backend.service.ChainActivity
import squad.nononotaa.backend.service.ChainTransfer
import squad.nononotaa.backend.service.ExclusionLabelDirectory
import squad.nononotaa.backend.service.OffRampLabel
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Instant

// Repeatable demo chain data for the demo-v1 addresses only (D23-007); every other address is left to the live sources
class SyntheticActivitySource(
    val fixture: SyntheticActivityFixture,
) : AddressActivitySource {
    override val sourceName = "synthetic"

    override val synthetic = true

    private val labelSource = "Synthetic fixture ${fixture.version}"

    private val byAddress = fixture.addresses.associateBy { ExclusionLabelDirectory.canonical(it.network, it.address) }

    override fun covers(address: CanonicalAddress) = address in byAddress

    override fun fetch(address: CanonicalAddress): ChainActivity? = byAddress[address]?.let { entry ->
        ChainActivity(unit = entry.network.name, transfers = entry.transfers.map(::toTransfer), complete = true)
    }

    private fun toTransfer(transfer: FixtureTransfer) = ChainTransfer(
        transactionId = transfer.transactionId,
        occurredAt = Instant.parse(transfer.occurredAt),
        direction = transfer.direction,
        amount = BigDecimal(transfer.amount),
        unit = transfer.unit,
        tokenContract = transfer.tokenContract,
        counterparty = transfer.counterparty,
        counterpartyLabel = transfer.counterpartyLabel?.let { OffRampLabel(it.kind, it.name, labelSource) },
    )

    companion object {
        const val FIXTURE_RESOURCE = "fixtures/demo-v1-activity.json"

        @JvmStatic
        fun load(objectMapper: ObjectMapper): SyntheticActivitySource {
            val fixture = objectMapper.readValue(ClassPathResource(FIXTURE_RESOURCE).inputStream, SyntheticActivityFixture::class.java)
            require(fixture.synthetic) { "$FIXTURE_RESOURCE must be marked synthetic" }
            require(fixture.addresses.flatMap { it.transfers }.none { it.counterpartyLabel?.kind == CashOutSignalKind.STABLECOIN_SWAP }) {
                "$FIXTURE_RESOURCE: a counterparty cannot be a stablecoin swap"
            }
            return SyntheticActivitySource(fixture)
        }
    }
}
