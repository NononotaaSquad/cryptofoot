package squad.nononotaa.backend.service

import squad.nononotaa.backend.analysis.AddressActivity
import squad.nononotaa.backend.analysis.CashOutSignal
import squad.nononotaa.backend.model.AddressActivityCoverageLimitation
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.CashOutSignalKind
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.TransferDirection
import java.math.BigDecimal
import java.time.Instant

// Turns what a source read from the chain into Address Activity: totals and cash-out signals (D23-004, D23-006)
class AddressActivityEvaluator(
    private val labels: ExclusionLabelDirectory,
    private val stablecoinSwapThreshold: BigDecimal,
) {
    fun evaluate(
        address: CanonicalAddress,
        source: AddressActivitySource,
        chain: ChainActivity,
        checkedAt: Instant,
    ): AddressActivity {
        val coin = chain.transfers.filter { it.tokenContract == null }
        val times = chain.transfers.map { it.occurredAt }
        return AddressActivity(
            sourceName = source.sourceName,
            synthetic = source.synthetic,
            checkedAt = checkedAt,
            unit = chain.unit,
            // a partial page never gives a total: missing data is not zero activity
            totalReceived = chain.totalReceived ?: coin.total(TransferDirection.IN).takeIf { chain.complete },
            totalSent = chain.totalSent ?: coin.total(TransferDirection.OUT).takeIf { chain.complete },
            transactionCount = chain.transactionCount ?: chain.transfers.map { it.transactionId }.distinct().size.takeIf { chain.complete },
            firstActivityAt = times.minOrNull()?.takeIf { chain.complete },
            lastActivityAt = times.maxOrNull(),
            cashOutSignals =
            (offRampTransfers(address.network, chain) + stablecoinSwaps(address.network, chain))
                .sortedWith(compareBy<CashOutSignal> { it.occurredAt }.thenBy { it.transactionId }),
            coverageLimitations =
            (
                listOfNotNull(
                    AddressActivityCoverageLimitation.RECENT_TRANSACTIONS_ONLY.takeIf { !chain.complete },
                    AddressActivityCoverageLimitation.LABELLED_COUNTERPARTIES_ONLY,
                ) + chain.limitations
                ).distinct().sorted(),
        )
    }

    private fun List<ChainTransfer>.total(direction: TransferDirection) = filter { it.direction == direction }.sumOf { it.amount }.plain()

    // an outgoing transfer to a labelled off-ramp is a signal at any amount
    private fun offRampTransfers(
        network: Network,
        chain: ChainActivity,
    ) = chain.transfers
        .filter { it.direction == TransferDirection.OUT && it.amount.signum() > 0 }
        .mapNotNull { transfer ->
            val label = transfer.counterparty?.let { offRampLabel(network, it) } ?: transfer.counterpartyLabel ?: return@mapNotNull null
            signal(label.kind, transfer, chain, "${label.source}: ${label.name}")
        }

    private fun offRampLabel(
        network: Network,
        counterparty: String,
    ) = labels
        .find(ExclusionLabelDirectory.canonical(network, counterparty))
        ?.takeIf { it.reason == ExclusionReason.EXCHANGE }
        ?.let { OffRampLabel(CashOutSignalKind.EXCHANGE, it.name, it.source) }

    // stablecoins are matched by contract, never by symbol, so a token named "USDC" proves nothing
    private fun stablecoinSwaps(
        network: Network,
        chain: ChainActivity,
    ) = chain.transfers
        .groupBy { it.transactionId }
        .values
        .filter { transfers -> transfers.any { it.direction == TransferDirection.OUT && !isUsdStablecoin(network, it.tokenContract) } }
        .flatMap { transfers ->
            transfers
                .filter { it.direction == TransferDirection.IN && isUsdStablecoin(network, it.tokenContract) }
                .groupBy { it.tokenContract }
                .values
                .map { bought -> bought.first().copy(amount = bought.sumOf { it.amount }.plain()) }
                .filter { it.amount >= stablecoinSwapThreshold }
                .map { signal(CashOutSignalKind.STABLECOIN_SWAP, it, chain, null) }
        }

    private fun isUsdStablecoin(
        network: Network,
        tokenContract: String?,
    ) = tokenContract != null && tokenContract.lowercase() in usdStablecoins[network].orEmpty()

    private fun signal(
        kind: CashOutSignalKind,
        transfer: ChainTransfer,
        chain: ChainActivity,
        counterpartyLabel: String?,
    ) = CashOutSignal(
        kind = kind,
        transactionId = transfer.transactionId,
        transactionUrl = chain.transactionUrlPrefix?.let { it + transfer.transactionId },
        occurredAt = transfer.occurredAt,
        amount = transfer.amount,
        unit = transfer.unit,
        counterpartyAddress = transfer.counterparty,
        counterpartyLabel = counterpartyLabel,
    )

    companion object {
        val usdStablecoins =
            mapOf(
                Network.ETH to
                    setOf(
                        "0xdac17f958d2ee523a2206206994597c13d831ec7", // USDT
                        "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", // USDC
                        "0x6b175474e89094c44da98b954eedeac495271d0f", // DAI
                    ),
            )
    }
}
