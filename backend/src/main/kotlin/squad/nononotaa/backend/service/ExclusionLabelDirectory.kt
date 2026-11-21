package squad.nononotaa.backend.service

import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.ExclusionLabel
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.Network
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.io.InputStream

/**
 * Offline label lists of addresses that must not be attributed to a person (decision D22-002).
 * Looking an address up here never sends it outside the application.
 */
class ExclusionLabelDirectory(
    private val labels: Map<CanonicalAddress, ExclusionLabel>,
) {
    val size: Int get() = labels.size

    fun find(address: CanonicalAddress): ExclusionLabel? = labels[address]

    data class EtherscanEntry(
        val name: String? = null,
        val labels: List<String> = emptyList(),
    )

    data class OwnEntry(
        val network: Network,
        val address: String,
        val reason: ExclusionReason,
        val name: String,
        val reference: String,
    )

    data class OwnList(
        val entries: List<OwnEntry>,
    )

    companion object {
        const val ETHERSCAN_SOURCE = "Etherscan labels (github.com/brianleect/etherscan-labels, MIT, scraped 2023-06-18)"
        const val OWN_SOURCE = "Own label list"

        /** Own entries win over the dataset, so the team can correct a wrong upstream tag. */
        @JvmStatic
        fun load(
            mapper: ObjectMapper,
            etherscanLabels: InputStream,
            ownLabels: InputStream,
        ): ExclusionLabelDirectory {
            val etherscan = fromEtherscan(etherscanLabels.use { mapper.readValue<Map<String, EtherscanEntry>>(it) })
            val own = fromOwnList(ownLabels.use { mapper.readValue<OwnList>(it) })
            return ExclusionLabelDirectory(etherscan + own)
        }

        @JvmStatic
        fun fromEtherscan(entries: Map<String, EtherscanEntry>): Map<CanonicalAddress, ExclusionLabel> = entries.entries
            .sortedBy { it.key }
            .mapNotNull { (address, entry) ->
                EtherscanTagMapping.reasonFor(entry.labels)?.let { reason ->
                    val tags = entry.labels.sorted().joinToString(", ")
                    val name = entry.name?.takeIf { it.isNotBlank() } ?: "no name"
                    CanonicalAddress(Network.ETH, address.lowercase()) to ExclusionLabel(reason, "$name [$tags]", ETHERSCAN_SOURCE)
                }
            }
            // upstream keeps a few addresses twice with different letter case: the first by sorted key wins
            .distinctBy { it.first }
            .toMap()

        @JvmStatic
        fun fromOwnList(list: OwnList): Map<CanonicalAddress, ExclusionLabel> = list.entries.associate {
            canonical(it.network, it.address) to ExclusionLabel(it.reason, it.name, "$OWN_SOURCE (${it.reference})")
        }

        /** Same canonical form as [AddressValidator]: ETH and bech32 lowercased, Base58 unchanged (ADR 0002). */
        @JvmStatic
        fun canonical(
            network: Network,
            address: String,
        ): CanonicalAddress {
            val trimmed = address.trim()
            val lowercase = network == Network.ETH || trimmed.lowercase().startsWith("bc1")
            return CanonicalAddress(network, if (lowercase) trimmed.lowercase() else trimmed)
        }
    }
}

/**
 * Maps Etherscan tags to an [ExclusionReason] (decisions D22-002, D22-006). Only explicit tags count. Tags that can mark a
 * private person (ofac-sanctions-lists, gitcoin-grants, maker-vault-owner, buidlguidl-builders, sybil-delegate, ...)
 * are deliberately absent. Contracts need no tag: the bytecode check finds them.
 */
object EtherscanTagMapping {
    val mixerTags = setOf("tornado-cash", "typhoon-cash", "ethereum-mixer")

    val exchangeTags =
        setOf(
            // centralised exchanges and their wallets
            "abcc", "allbit", "ascendex", "bgogo", "binance", "binance-deposit", "bitcoin-suisse", "bitfinex", "bithumb",
            "bitmart", "bitmex", "bitstamp", "bittrex", "cobinhood", "coinbase", "coinhako", "coinmetro", "coinone",
            "coinsquare", "coss-io", "crex24", "crypto-com", "deribit", "digifinex", "ftx", "gate-io", "gemini", "hitbtc",
            "hoo-com", "hotbit", "huobi", "korbit", "kraken", "kucoin", "latoken", "liquid", "maskex", "okx", "paribu",
            "poloniex", "quadrigacx", "tidex", "topbtc", "upbit", "yunbi", "zb-com",
            // exchange wallets without an exchange tag
            "hot-wallet", "cold-wallet",
            // custodial services, crypto-fiat gateways and OTC desks (brief: CEX / Custodial, CASP / Off-Ramp)
            "blockfi", "celsius-network", "changenow", "fiat-gateway", "nexo", "otc", "remitano", "wirex",
        )

    val fundraiserTags = setOf("donate", "crowdfunding")

    /** Deterministic precedence when an entry has tags of several reasons. */
    fun reasonFor(tags: Collection<String>): ExclusionReason? = when {
        tags.any { it in mixerTags } -> ExclusionReason.MIXER
        tags.any { it in exchangeTags } -> ExclusionReason.EXCHANGE
        tags.any { it in fundraiserTags } -> ExclusionReason.FUNDRAISER
        else -> null
    }
}
