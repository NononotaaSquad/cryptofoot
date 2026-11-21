package squad.nononotaa.backend.util

import org.bouncycastle.crypto.digests.KeccakDigest
import java.math.BigInteger
import java.security.MessageDigest

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = BigInteger.valueOf(58)

    fun decode(input: String): ByteArray? {
        var value = BigInteger.ZERO
        for (char in input) {
            val digit = ALPHABET.indexOf(char)
            if (digit < 0) return null
            value = value.multiply(BASE).add(BigInteger.valueOf(digit.toLong()))
        }
        val leadingZeros = ByteArray(input.takeWhile { it == '1' }.length)
        if (value.signum() == 0) return leadingZeros
        val bytes = value.toByteArray()
        // BigInteger adds a sign byte when the highest bit is set
        val unsigned = if (bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        return leadingZeros + unsigned
    }

    fun hasValidChecksum(decoded: ByteArray): Boolean {
        if (decoded.size < 5) return false
        val payload = decoded.copyOfRange(0, decoded.size - 4)
        val expected = sha256(sha256(payload)).copyOfRange(0, 4)
        return expected.contentEquals(decoded.copyOfRange(decoded.size - 4, decoded.size))
    }

    private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
}

object Bech32 {
    enum class Encoding(
        val constant: Int,
    ) {
        BECH32(1),
        BECH32M(0x2bc830a3),
    }

    data class Decoded(
        val hrp: String,
        val data: List<Int>,
        val encoding: Encoding?,
    )

    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private const val MAX_LENGTH = 90
    private const val CHECKSUM_LENGTH = 6
    private val GENERATOR = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

    fun decode(lowercase: String): Decoded? {
        if (lowercase.length > MAX_LENGTH || lowercase.any { it.code !in 33..126 }) return null
        val separator = lowercase.lastIndexOf('1')
        if (separator < 1 || separator + CHECKSUM_LENGTH + 1 > lowercase.length) return null
        val hrp = lowercase.substring(0, separator)
        val values = lowercase.substring(separator + 1).map { CHARSET.indexOf(it) }
        if (values.any { it < 0 }) return null
        val checksum = polymod(expandHrp(hrp) + values)
        val encoding = Encoding.entries.firstOrNull { it.constant == checksum }
        return Decoded(hrp, values.dropLast(CHECKSUM_LENGTH), encoding)
    }

    fun convertBits(
        data: List<Int>,
        from: Int,
        to: Int,
    ): ByteArray? {
        var accumulator = 0
        var bits = 0
        val maxValue = (1 shl to) - 1
        val result = mutableListOf<Byte>()
        for (value in data) {
            if (value ushr from != 0) return null
            accumulator = (accumulator shl from) or value
            bits += from
            while (bits >= to) {
                bits -= to
                result += ((accumulator ushr bits) and maxValue).toByte()
            }
        }
        if (bits >= from || ((accumulator shl (to - bits)) and maxValue) != 0) return null
        return result.toByteArray()
    }

    private fun expandHrp(hrp: String): List<Int> = hrp.map { it.code ushr 5 } + 0 + hrp.map { it.code and 31 }

    private fun polymod(values: List<Int>): Int {
        var checksum = 1
        for (value in values) {
            val top = checksum ushr 25
            checksum = ((checksum and 0x1ffffff) shl 5) xor value
            GENERATOR.forEachIndexed { i, generator -> if ((top ushr i) and 1 == 1) checksum = checksum xor generator }
        }
        return checksum
    }
}

object Keccak {
    fun keccak256(bytes: ByteArray): ByteArray {
        val digest = KeccakDigest(256)
        digest.update(bytes, 0, bytes.size)
        return ByteArray(32).also { digest.doFinal(it, 0) }
    }
}
