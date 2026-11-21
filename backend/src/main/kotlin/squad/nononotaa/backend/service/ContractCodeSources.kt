package squad.nononotaa.backend.service

import squad.nononotaa.backend.model.ContractCodeCheck
import squad.nononotaa.backend.util.logger
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** A keyless public service that tells whether an ETH address holds executable code (decision D22-003). */
interface ContractCodeSource {
    /** Shown to the Analyst as the classification source of a SMART_CONTRACT exclusion. */
    val sourceName: String

    fun check(address: String): ContractCodeCheck
}

/** EIP-7702: an account delegating to a contract carries exactly `0xef0100` + 20-byte address; it stays a private account (D22-004). */
private val delegationDesignator = Regex("^0xef0100[0-9a-f]{40}$", RegexOption.IGNORE_CASE)

internal fun codeCheckOf(code: String): ContractCodeCheck = when {
    code.equals("0x", ignoreCase = true) -> ContractCodeCheck.ACCOUNT
    delegationDesignator.matches(code) -> ContractCodeCheck.ACCOUNT
    else -> ContractCodeCheck.CONTRACT
}

private const val USER_AGENT = "osint-analysis-backend"

/** Sends a request; null when the service does not answer with HTTP 200 within the timeout. */
private fun HttpClient.bodyOrNull(
    request: HttpRequest,
    sourceName: String,
): String? = try {
    val response = send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() == 200) {
        response.body()
    } else {
        ContractCodeSourceLog.logger.warn("Contract code source {} answered HTTP {}", sourceName, response.statusCode())
        null
    }
} catch (e: IOException) {
    ContractCodeSourceLog.logger.warn("Contract code source {} is unavailable: {}", sourceName, e.toString())
    null
} catch (e: InterruptedException) {
    Thread.currentThread().interrupt()
    null
}

private object ContractCodeSourceLog {
    val logger = logger()
}

/** `eth_getCode` on a public Ethereum JSON-RPC endpoint (no API key). */
class EthRpcContractCodeSource(
    private val endpoint: URI,
    private val timeout: Duration,
    private val mapper: ObjectMapper,
    private val http: HttpClient,
) : ContractCodeSource {
    override val sourceName = "Ethereum JSON-RPC eth_getCode (${endpoint.host})"

    override fun check(address: String): ContractCodeCheck {
        val payload = """{"jsonrpc":"2.0","id":1,"method":"eth_getCode","params":["$address","latest"]}"""
        val request =
            HttpRequest
                .newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build()
        val body = http.bodyOrNull(request, sourceName) ?: return ContractCodeCheck.UNAVAILABLE
        val result = mapper.readTreeOrNull(body)?.path("result")
        if (result == null || !result.isString || !result.stringValue().startsWith("0x")) {
            logger.warn("Contract code source {} gave no usable result", sourceName)
            return ContractCodeCheck.UNAVAILABLE
        }
        return codeCheckOf(result.stringValue())
    }

    companion object {
        private val logger = logger()
    }
}

/** Blockchair Ethereum address dashboard (no API key, rate-limited): the fallback when the RPC is unavailable. */
class BlockchairContractCodeSource(
    private val baseUrl: URI,
    private val timeout: Duration,
    private val mapper: ObjectMapper,
    private val http: HttpClient,
) : ContractCodeSource {
    override val sourceName = "Blockchair Ethereum address dashboard (${baseUrl.host})"

    override fun check(address: String): ContractCodeCheck {
        val request =
            HttpRequest
                .newBuilder(baseUrl.resolve("/ethereum/dashboards/address/$address"))
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build()
        val body = http.bodyOrNull(request, sourceName) ?: return ContractCodeCheck.UNAVAILABLE
        val root = mapper.readTreeOrNull(body) ?: return ContractCodeCheck.UNAVAILABLE
        val details = root.path("data").values().firstOrNull()?.path("address")
        if (root.path("context").path("code").asInt() != 200 || details == null) {
            logger.warn("Contract code source {} gave no usable result (code {})", sourceName, root.path("context").path("code").asInt())
            return ContractCodeCheck.UNAVAILABLE
        }
        val code = details.path("contract_code_hex")
        return when (details.path("type").asString("")) {
            "account" -> ContractCodeCheck.ACCOUNT
            "contract" -> if (code.isString) codeCheckOf(normalizedHex(code.stringValue())) else ContractCodeCheck.CONTRACT
            else -> ContractCodeCheck.UNAVAILABLE
        }
    }

    private fun normalizedHex(code: String) = if (code.startsWith("0x")) code else "0x$code"

    companion object {
        private val logger = logger()
    }
}

private fun ObjectMapper.readTreeOrNull(body: String): JsonNode? = try {
    readTree(body)
} catch (e: Exception) {
    null
}
