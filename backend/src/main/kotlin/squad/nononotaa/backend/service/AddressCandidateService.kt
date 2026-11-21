package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.model.AddressCandidate
import squad.nononotaa.backend.model.AddressCandidates
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.MentionAddresses
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.RejectedCandidate
import squad.nononotaa.backend.model.RejectionReason
import squad.nononotaa.backend.util.Base58Check
import squad.nononotaa.backend.util.Bech32
import squad.nononotaa.backend.util.Bech32Encoding
import squad.nononotaa.backend.util.Keccak256
import squad.nononotaa.backend.util.logger

@Service
class AddressCandidateService {
    fun prepare(mentions: List<MentionAddresses>): AddressCandidates {
        logger.debug("Preparing address candidates from mentions={}", mentions.size)
        val mentionIdsByAddress = mutableMapOf<CanonicalAddress, MutableSet<String>>()
        val rejected = mutableListOf<RejectedCandidate>()
        for (mention in mentions) {
            for (value in mention.detectedAddresses) {
                when (val result = validate(value)) {
                    is Validation.Valid -> mentionIdsByAddress.getOrPut(result.address) { sortedSetOf() } += mention.mentionId
                    is Validation.Invalid -> rejected += RejectedCandidate(mention.mentionId, value, result.network, result.reason)
                }
            }
        }
        val valid =
            mentionIdsByAddress
                .map { (address, mentionIds) -> AddressCandidate(address, mentionIds.toList()) }
                .sortedWith(compareBy({ it.address.network }, { it.address.address }))
        logger.info("Prepared address candidates valid={} rejected={}", valid.size, rejected.size)
        return AddressCandidates(valid, rejected)
    }

    fun validate(value: String): Validation {
        val candidate = value.trim()
        return when {
            ETHEREUM_SHAPE.matches(candidate) -> validateEthereum(candidate)
            BITCOIN_BASE58_SHAPE.matches(candidate) -> validateBitcoinBase58(candidate)
            BITCOIN_BECH32_SHAPE.matches(candidate) -> validateBitcoinBech32(candidate)
            else -> Validation.Invalid(null, RejectionReason.UNKNOWN_FORMAT)
        }
    }

    private fun validateEthereum(value: String): Validation {
        val hex = value.substring(2)
        val hasChecksum = hex != hex.lowercase() && hex != hex.uppercase()
        if (hasChecksum && hex != eip55(hex.lowercase())) return Validation.Invalid(Network.ETHEREUM, RejectionReason.INVALID_ADDRESS)
        return Validation.Valid(CanonicalAddress(Network.ETHEREUM, "0x${hex.lowercase()}"))
    }

    private fun eip55(lowerHex: String): String {
        val hash = Keccak256.hash(lowerHex.toByteArray(Charsets.US_ASCII))
        return lowerHex
            .mapIndexed { i, char ->
                val nibble = (hash[i / 2].toInt() shr (if (i % 2 == 0) 4 else 0)) and 0xf
                if (nibble >= 8) char.uppercaseChar() else char
            }.joinToString("")
    }

    private fun validateBitcoinBase58(value: String): Validation {
        val payload = Base58Check.decode(value)
        val valid = payload != null && payload.size == 21 && payload[0].toInt() in BITCOIN_BASE58_VERSIONS
        return if (valid) {
            Validation.Valid(CanonicalAddress(Network.BITCOIN, value))
        } else {
            Validation.Invalid(Network.BITCOIN, RejectionReason.INVALID_ADDRESS)
        }
    }

    private fun validateBitcoinBech32(value: String): Validation {
        val invalid = Validation.Invalid(Network.BITCOIN, RejectionReason.INVALID_ADDRESS)
        val decoded = Bech32.decode(value)?.takeIf { it.hrp == "bc" && it.data.isNotEmpty() } ?: return invalid
        val witnessVersion = decoded.data.first()
        val program = Bech32.convertBits(decoded.data.drop(1), 5, 8) ?: return invalid
        val expectedEncoding = if (witnessVersion == 0) Bech32Encoding.BECH32 else Bech32Encoding.BECH32M
        val valid =
            witnessVersion in 0..16 &&
                decoded.encoding == expectedEncoding &&
                program.size in 2..40 &&
                (witnessVersion != 0 || program.size == 20 || program.size == 32)
        return if (valid) Validation.Valid(CanonicalAddress(Network.BITCOIN, value.lowercase())) else invalid
    }

    sealed interface Validation {
        data class Valid(
            val address: CanonicalAddress,
        ) : Validation

        data class Invalid(
            val network: Network?,
            val reason: RejectionReason,
        ) : Validation
    }

    companion object {
        private val logger = logger()
        private val ETHEREUM_SHAPE = Regex("^0x[0-9a-fA-F]{40}$")
        private val BITCOIN_BASE58_SHAPE = Regex("^[13][1-9A-HJ-NP-Za-km-z]{25,34}$")
        private val BITCOIN_BECH32_SHAPE = Regex("^(bc1|BC1)[0-9a-zA-Z]{6,87}$")
        private val BITCOIN_BASE58_VERSIONS = setOf(0x00, 0x05)
    }
}
