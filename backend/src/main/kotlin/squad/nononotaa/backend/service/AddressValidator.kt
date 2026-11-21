package squad.nononotaa.backend.service

import org.springframework.stereotype.Component
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.AddressValidationFailure
import squad.nononotaa.backend.model.AddressValidationFailure.CHECKSUM_MISMATCH
import squad.nononotaa.backend.model.AddressValidationFailure.MALFORMED
import squad.nononotaa.backend.model.AddressValidationFailure.NETWORK_MISMATCH
import squad.nononotaa.backend.model.AddressValidationFailure.UNSUPPORTED_NETWORK
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.util.Base58
import squad.nononotaa.backend.util.Bech32
import squad.nononotaa.backend.util.Keccak

@Component
class AddressValidator {
    fun validate(
        rawAddress: String,
        networkHint: Network? = null,
    ): AddressValidation {
        val result = validateFormat(rawAddress.trim())
        if (result is AddressValidation.Valid && networkHint != null && networkHint != result.canonicalAddress.network) {
            return AddressValidation.Invalid(NETWORK_MISMATCH)
        }
        return result
    }

    private fun validateFormat(address: String): AddressValidation {
        val lowercase = address.lowercase()
        return when {
            address.startsWith("0x") -> validateEthereum(address)
            SEGWIT_PREFIXES.any { lowercase.startsWith(it) } -> validateSegwit(address)
            address.isNotEmpty() && address[0] in BASE58_PREFIXES -> validateBase58(address)
            else -> invalid(UNSUPPORTED_NETWORK)
        }
    }

    private fun validateEthereum(address: String): AddressValidation {
        val hex = address.removePrefix("0x")
        if (!HEX_40.matches(hex)) return invalid(MALFORMED)
        val hasChecksum = hex != hex.lowercase() && hex != hex.uppercase()
        if (hasChecksum && hex != eip55(hex)) return invalid(CHECKSUM_MISMATCH)
        return valid(Network.ETH, "0x" + hex.lowercase())
    }

    private fun eip55(hex: String): String {
        val lowercase = hex.lowercase()
        val hash = Keccak.keccak256(lowercase.toByteArray(Charsets.US_ASCII))
        return lowercase
            .mapIndexed { i, char ->
                val nibble = (hash[i / 2].toInt() ushr (if (i % 2 == 0) 4 else 0)) and 0xf
                if (char.isLetter() && nibble >= 8) char.uppercaseChar() else char
            }.joinToString("")
    }

    private fun validateSegwit(address: String): AddressValidation {
        val lowercase = address.lowercase()
        if (address != lowercase && address != address.uppercase()) return invalid(MALFORMED)
        if (!lowercase.startsWith("bc1")) return invalid(UNSUPPORTED_NETWORK)
        val decoded = Bech32.decode(lowercase) ?: return invalid(MALFORMED)
        if (decoded.hrp != "bc") return invalid(MALFORMED)
        val encoding = decoded.encoding ?: return invalid(CHECKSUM_MISMATCH)
        val version = decoded.data.firstOrNull() ?: return invalid(MALFORMED)
        if (version > 16) return invalid(MALFORMED)
        val program = Bech32.convertBits(decoded.data.drop(1), 5, 8) ?: return invalid(MALFORMED)
        if (program.size !in 2..40 || (version == 0 && program.size != 20 && program.size != 32)) return invalid(MALFORMED)
        val expectedEncoding = if (version == 0) Bech32.Encoding.BECH32 else Bech32.Encoding.BECH32M
        if (encoding != expectedEncoding) return invalid(CHECKSUM_MISMATCH)
        return valid(Network.BTC, lowercase)
    }

    private fun validateBase58(address: String): AddressValidation {
        val decoded = Base58.decode(address) ?: return invalid(MALFORMED)
        if (decoded.size != BASE58_ADDRESS_LENGTH) return invalid(MALFORMED)
        if (!Base58.hasValidChecksum(decoded)) return invalid(CHECKSUM_MISMATCH)
        return when (decoded[0].toInt() and 0xff) {
            in BTC_MAINNET_VERSIONS -> valid(Network.BTC, address)
            in BTC_TESTNET_VERSIONS -> invalid(UNSUPPORTED_NETWORK)
            else -> invalid(MALFORMED)
        }
    }

    private fun valid(
        network: Network,
        address: String,
    ) = AddressValidation.Valid(CanonicalAddress(network, address))

    private fun invalid(failure: AddressValidationFailure) = AddressValidation.Invalid(failure)

    companion object {
        private val HEX_40 = Regex("[0-9a-fA-F]{40}")
        private val SEGWIT_PREFIXES = listOf("bc1", "tb1", "bcrt1")
        private val BASE58_PREFIXES = setOf('1', '3', 'm', 'n', '2')
        private const val BASE58_ADDRESS_LENGTH = 25
        private val BTC_MAINNET_VERSIONS = setOf(0x00, 0x05)
        private val BTC_TESTNET_VERSIONS = setOf(0x6f, 0xc4)
    }
}
