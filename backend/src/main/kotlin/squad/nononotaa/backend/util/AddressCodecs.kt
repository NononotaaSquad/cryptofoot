package squad.nononotaa.backend.util

import java.math.BigInteger
import java.security.MessageDigest

object Base58Check {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = BigInteger.valueOf(58)

    fun decode(value: String): ByteArray? {
        if (value.isEmpty()) return null
        var number = BigInteger.ZERO
        for (char in value) {
            val digit = ALPHABET.indexOf(char)
            if (digit < 0) return null
            number = number.multiply(BASE).add(BigInteger.valueOf(digit.toLong()))
        }
        val leadingZeros = value.takeWhile { it == '1' }.length
        val body = number.toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
        val bytes = ByteArray(leadingZeros) + body
        if (bytes.size < 5) return null
        val payload = bytes.copyOfRange(0, bytes.size - 4)
        val checksum = bytes.copyOfRange(bytes.size - 4, bytes.size)
        return payload.takeIf { sha256(sha256(it)).copyOfRange(0, 4).contentEquals(checksum) }
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
}

enum class Bech32Encoding(val constant: Int) { BECH32(1), BECH32M(0x2bc830a3) }

data class Bech32Data(
    val hrp: String,
    val data: List<Int>,
    val encoding: Bech32Encoding,
)

object Bech32 {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val GENERATOR = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

    fun decode(value: String): Bech32Data? {
        if (value.length > 90 || value.any { it.code !in 33..126 }) return null
        if (value != value.lowercase() && value != value.uppercase()) return null
        val lower = value.lowercase()
        val separator = lower.lastIndexOf('1')
        if (separator < 1 || separator + 7 > lower.length) return null
        val hrp = lower.substring(0, separator)
        val data = lower.substring(separator + 1).map { CHARSET.indexOf(it) }
        if (data.any { it < 0 }) return null
        val encoding = Bech32Encoding.entries.find { polymod(expandHrp(hrp) + data) == it.constant } ?: return null
        return Bech32Data(hrp, data.dropLast(6), encoding)
    }

    fun convertBits(
        data: List<Int>,
        from: Int,
        to: Int,
    ): List<Int>? {
        var accumulator = 0
        var bits = 0
        val result = mutableListOf<Int>()
        val mask = (1 shl to) - 1
        for (value in data) {
            accumulator = (accumulator shl from) or value
            bits += from
            while (bits >= to) {
                bits -= to
                result += (accumulator shr bits) and mask
            }
        }
        if (bits >= from || ((accumulator shl (to - bits)) and mask) != 0) return null
        return result
    }

    private fun expandHrp(hrp: String) = hrp.map { it.code shr 5 } + 0 + hrp.map { it.code and 31 }

    private fun polymod(values: List<Int>): Int {
        var checksum = 1
        for (value in values) {
            val top = checksum ushr 25
            checksum = ((checksum and 0x1ffffff) shl 5) xor value
            for (i in GENERATOR.indices) {
                if ((top shr i) and 1 == 1) checksum = checksum xor GENERATOR[i]
            }
        }
        return checksum
    }
}

object Keccak256 {
    private const val RATE = 136
    private val ROUND_CONSTANTS =
        listOf(
            0x0000000000000001uL, 0x0000000000008082uL, 0x800000000000808AuL, 0x8000000080008000uL,
            0x000000000000808BuL, 0x0000000080000001uL, 0x8000000080008081uL, 0x8000000000008009uL,
            0x000000000000008AuL, 0x0000000000000088uL, 0x0000000080008009uL, 0x000000008000000AuL,
            0x000000008000808BuL, 0x800000000000008BuL, 0x8000000000008089uL, 0x8000000000008003uL,
            0x8000000000008002uL, 0x8000000000000080uL, 0x000000000000800AuL, 0x800000008000000AuL,
            0x8000000080008081uL, 0x8000000000008080uL, 0x0000000080000001uL, 0x8000000080008008uL,
        ).map { it.toLong() }
    private val ROTATIONS =
        intArrayOf(0, 1, 62, 28, 27, 36, 44, 6, 55, 20, 3, 10, 43, 25, 39, 41, 45, 15, 21, 8, 18, 2, 61, 56, 14)

    fun hash(input: ByteArray): ByteArray {
        val padded = input.copyOf(((input.size / RATE) + 1) * RATE)
        padded[input.size] = (padded[input.size].toInt() xor 0x01).toByte()
        padded[padded.size - 1] = (padded[padded.size - 1].toInt() xor 0x80).toByte()
        val state = LongArray(25)
        for (block in padded.indices step RATE) {
            for (lane in 0 until RATE / 8) state[lane] = state[lane] xor readLane(padded, block + lane * 8)
            permute(state)
        }
        return ByteArray(32) { (state[it / 8] ushr (8 * (it % 8))).toByte() }
    }

    private fun readLane(
        bytes: ByteArray,
        offset: Int,
    ) = (0 until 8).fold(0L) { lane, i -> lane or ((bytes[offset + i].toLong() and 0xff) shl (8 * i)) }

    private fun permute(state: LongArray) {
        val c = LongArray(5)
        val b = LongArray(25)
        for (roundConstant in ROUND_CONSTANTS) {
            for (x in 0 until 5) c[x] = state[x] xor state[x + 5] xor state[x + 10] xor state[x + 15] xor state[x + 20]
            for (x in 0 until 5) {
                val d = c[(x + 4) % 5] xor c[(x + 1) % 5].rotateLeft(1)
                for (y in 0 until 25 step 5) state[x + y] = state[x + y] xor d
            }
            for (x in 0 until 5) {
                for (y in 0 until 5) b[y + 5 * ((2 * x + 3 * y) % 5)] = state[x + 5 * y].rotateLeft(ROTATIONS[x + 5 * y])
            }
            for (x in 0 until 5) {
                for (y in 0 until 25 step 5) state[x + y] = b[x + y] xor (b[(x + 1) % 5 + y].inv() and b[(x + 2) % 5 + y])
            }
            state[0] = state[0] xor roundConstant
        }
    }
}
