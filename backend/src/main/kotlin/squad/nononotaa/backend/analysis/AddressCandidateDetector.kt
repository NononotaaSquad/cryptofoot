package squad.nononotaa.backend.analysis

import org.springframework.stereotype.Component
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.RawAddressCandidate

@Component
class AddressCandidateDetector {
    fun detect(
        mentionId: String,
        text: String,
    ): List<RawAddressCandidate> = PATTERN.findAll(text).map { match ->
        val (raw, network) =
            when {
                match.groups["btcBase58"] != null -> match.value to Network.BTC
                match.groups["btcBech32"] != null -> match.value to Network.BTC
                else -> match.value to Network.ETH
            }
        RawAddressCandidate(mentionId, raw, network)
    }.toList()

    companion object {
        private val PATTERN =
            Regex(
                "(?<![A-Za-z0-9])" +
                    "(?:(?<btcBase58>[13][a-km-zA-HJ-NP-Z1-9]{25,34})" +
                    // BIP-173 allows all-uppercase bech32; mixed case is left to the validator (MALFORMED)
                    "|(?<btcBech32>(?i:bc1[qpzry9x8gf2tvdw0s3jn54khce6mua7l]{11,71}))" +
                    "|(?<evm>0x[0-9a-fA-F]{40}))" +
                    "(?![A-Za-z0-9])",
            )
    }
}
