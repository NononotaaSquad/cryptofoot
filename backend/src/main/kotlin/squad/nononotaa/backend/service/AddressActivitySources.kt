package squad.nononotaa.backend.service

import squad.nononotaa.backend.model.AddressActivityCoverageLimitation
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.CashOutSignalKind
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.TransferDirection
import squad.nononotaa.backend.util.EXTERNAL_USER_AGENT
import squad.nononotaa.backend.util.bodyOrNull
import squad.nononotaa.backend.util.logger
import squad.nononotaa.backend.util.readTreeOrNull
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration
import java.time.Instant

data class OffRampLabel(
    val kind: CashOutSignalKind,
    val name: String,
    val source: String,
)

data class ChainTransfer(
    val transactionId: String,
    val occurredAt: Instant,
    val direction: TransferDirection,
    val amount: BigDecimal,
    val unit: String,
    // null for the network's own coin
    val tokenContract: String? = null,
    val counterparty: String? = null,
    // only a synthetic fixture names its counterparties; live data is labelled from the offline lists
    val counterpartyLabel: OffRampLabel? = null,
)

data class ChainActivity(
    val unit: String,
    val transfers: List<ChainTransfer>,
    // the transfers cover the whole history of the address, not only the most recent page
    val complete: Boolean,
    val totalReceived: BigDecimal? = null,
    val totalSent: BigDecimal? = null,
    val transactionCount: Int? = null,
    val transactionUrlPrefix: String? = null,
    val limitations: List<AddressActivityCoverageLimitation> = emptyList(),
)

interface AddressActivitySource {
    val sourceName: String
    val synthetic: Boolean

    fun covers(address: CanonicalAddress): Boolean

    // null when the source gave no usable answer
    fun fetch(address: CanonicalAddress): ChainActivity?
}

private const val SATOSHIS_PER_BTC = 8
private const val WEI_PER_ETH = 18

internal fun BigDecimal.plain(): BigDecimal = stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }

private fun JsonNode.longOrNull(): Long? = when {
    isIntegralNumber -> longValue()
    isString -> stringValue().toLongOrNull()
    else -> null
}

private fun JsonNode.textOrNull(): String? = if (isString) stringValue() else null

abstract class ExplorerSource(
    protected val baseUrl: URI,
    private val timeout: Duration,
    private val mapper: ObjectMapper,
    private val http: HttpClient,
) : AddressActivitySource {
    override val synthetic = false

    protected val transactionUrlPrefix: String get() = baseUrl.resolve("/tx/").toString()

    protected fun get(pathAndQuery: String): JsonNode? {
        val request =
            HttpRequest
                .newBuilder(URI.create(baseUrl.toString().trimEnd('/') + pathAndQuery))
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("User-Agent", EXTERNAL_USER_AGENT)
                .GET()
                .build()
        return http.bodyOrNull(request, sourceName)?.let(mapper::readTreeOrNull)
    }
}

// mempool.space REST API: keyless, open source, self-hostable (D23-003)
class MempoolActivitySource(
    baseUrl: URI,
    timeout: Duration,
    mapper: ObjectMapper,
    http: HttpClient,
) : ExplorerSource(baseUrl, timeout, mapper, http) {
    override val sourceName = "mempool.space Bitcoin explorer API (${baseUrl.host})"

    override fun covers(address: CanonicalAddress) = address.network == Network.BTC

    override fun fetch(address: CanonicalAddress): ChainActivity? {
        val stats = get("/api/address/${address.address}")?.path("chain_stats")
        val transactionCount = stats?.path("tx_count")?.longOrNull()
        val received = stats?.path("funded_txo_sum")?.longOrNull()
        val sent = stats?.path("spent_txo_sum")?.longOrNull()
        val txs = if (transactionCount != null && received != null && sent != null) get("/api/address/${address.address}/txs") else null
        if (transactionCount == null || received == null || sent == null || txs == null || !txs.isArray) {
            logger.warn("Address Activity source {} gave no usable result", sourceName)
            return null
        }
        val confirmed = txs.values().filter { it.path("status").path("confirmed").booleanValue(false) }
        return ChainActivity(
            unit = "BTC",
            transfers = confirmed.flatMap { transfersOf(it, address) },
            complete = confirmed.size >= transactionCount,
            totalReceived = btc(received),
            totalSent = btc(sent),
            transactionCount = transactionCount.toInt(),
            transactionUrlPrefix = transactionUrlPrefix,
        )
    }

    private fun transfersOf(
        tx: JsonNode,
        owner: CanonicalAddress,
    ): List<ChainTransfer> {
        val id = tx.path("txid").textOrNull() ?: return emptyList()
        val at = tx.path("status").path("block_time").longOrNull()?.let(Instant::ofEpochSecond) ?: return emptyList()
        val isOwner = { node: JsonNode -> node.path("scriptpubkey_address").textOrNull()?.let { canonical(it) } == owner.address }
        val inputs = tx.path("vin").values().map { it.path("prevout") }
        val outputs = tx.path("vout").values()
        val spent = inputs.filter(isOwner).sumOf { it.path("value").longOrNull() ?: 0 }
        if (spent > 0) {
            return outputs
                .filterNot(isOwner)
                .mapNotNull { output ->
                    val to = output.path("scriptpubkey_address").textOrNull() ?: return@mapNotNull null
                    ChainTransfer(id, at, TransferDirection.OUT, btc(output.path("value").longOrNull() ?: 0), "BTC", counterparty = canonical(to))
                }
        }
        val received = outputs.filter(isOwner).sumOf { it.path("value").longOrNull() ?: 0 }
        if (received == 0L) return emptyList()
        val from = inputs.firstNotNullOfOrNull { it.path("scriptpubkey_address").textOrNull() }?.let { canonical(it) }
        return listOf(ChainTransfer(id, at, TransferDirection.IN, btc(received), "BTC", counterparty = from))
    }

    private fun canonical(address: String) = ExclusionLabelDirectory.canonical(Network.BTC, address).address

    private fun btc(satoshis: Long) = BigDecimal.valueOf(satoshis).movePointLeft(SATOSHIS_PER_BTC).plain()

    companion object {
        private val logger = logger()
    }
}

// Blockscout API v2: keyless, open source, self-hostable (D23-003)
class BlockscoutActivitySource(
    baseUrl: URI,
    timeout: Duration,
    mapper: ObjectMapper,
    http: HttpClient,
) : ExplorerSource(baseUrl, timeout, mapper, http) {
    override val sourceName = "Blockscout Ethereum explorer API (${baseUrl.host})"

    override fun covers(address: CanonicalAddress) = address.network == Network.ETH

    override fun fetch(address: CanonicalAddress): ChainActivity? {
        val base = "/api/v2/addresses/${address.address}"
        val transactionCount = get("$base/counters")?.path("transactions_count")?.longOrNull()
        val transactions = transactionCount?.let { get("$base/transactions") }
        val tokenTransfers = transactions?.let { get("$base/token-transfers?type=ERC-20") }
        if (transactionCount == null || transactions == null || tokenTransfers == null) {
            logger.warn("Address Activity source {} gave no usable result", sourceName)
            return null
        }
        val owner = address.address
        return ChainActivity(
            unit = "ETH",
            transfers =
            transactions.path("items").values().mapNotNull { coinTransfer(it, owner) } +
                tokenTransfers.path("items").values().mapNotNull { tokenTransfer(it, owner) },
            complete = isLastPage(transactions) && isLastPage(tokenTransfers),
            transactionCount = transactionCount.toInt(),
            transactionUrlPrefix = transactionUrlPrefix,
            limitations = listOf(AddressActivityCoverageLimitation.INTERNAL_TRANSFERS_NOT_COUNTED),
        )
    }

    private fun isLastPage(page: JsonNode) = page.path("next_page_params").let { it.isMissingNode || it.isNull }

    private fun coinTransfer(
        item: JsonNode,
        owner: String,
    ): ChainTransfer? {
        if (item.path("status").textOrNull() != "ok") return null
        val wei = item.path("value").textOrNull()?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null
        return transfer(item, item.path("hash"), owner, wei.movePointLeft(WEI_PER_ETH).plain(), "ETH", null)
    }

    private fun tokenTransfer(
        item: JsonNode,
        owner: String,
    ): ChainTransfer? {
        val token = item.path("token")
        val contract = (token.path("address_hash").textOrNull() ?: token.path("address").textOrNull())?.lowercase() ?: return null
        val decimals = (item.path("total").path("decimals").longOrNull() ?: token.path("decimals").longOrNull())?.toInt() ?: return null
        val raw = item.path("total").path("value").textOrNull()?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null
        val unit = token.path("symbol").textOrNull() ?: contract
        val hash = item.path("transaction_hash").takeUnless { it.isMissingNode || it.isNull } ?: item.path("tx_hash")
        return transfer(item, hash, owner, raw.movePointLeft(decimals).plain(), unit, contract)
    }

    private fun transfer(
        item: JsonNode,
        hash: JsonNode,
        owner: String,
        amount: BigDecimal,
        unit: String,
        tokenContract: String?,
    ): ChainTransfer? {
        val id = hash.textOrNull() ?: return null
        val at = item.path("timestamp").textOrNull()?.let(Instant::parse) ?: return null
        val from = item.path("from").path("hash").textOrNull()?.lowercase()
        val to = item.path("to").path("hash").textOrNull()?.lowercase()
        return when {
            from == owner && to == owner -> null
            from == owner -> ChainTransfer(id, at, TransferDirection.OUT, amount, unit, tokenContract, to)
            to == owner -> ChainTransfer(id, at, TransferDirection.IN, amount, unit, tokenContract, from)
            else -> null
        }
    }

    companion object {
        private val logger = logger()
    }
}
