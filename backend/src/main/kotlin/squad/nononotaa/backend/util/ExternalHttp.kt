package squad.nononotaa.backend.util

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val EXTERNAL_USER_AGENT = "osint-analysis-backend"

private object ExternalHttpLog {
    val logger = logger()
}

// null when the service does not answer with HTTP 200 within the timeout
fun HttpClient.bodyOrNull(
    request: HttpRequest,
    sourceName: String,
): String? = try {
    val response = send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() == 200) {
        response.body()
    } else {
        ExternalHttpLog.logger.warn("External source {} answered HTTP {}", sourceName, response.statusCode())
        null
    }
} catch (e: IOException) {
    ExternalHttpLog.logger.warn("External source {} is unavailable: {}", sourceName, e.toString())
    null
} catch (e: InterruptedException) {
    Thread.currentThread().interrupt()
    null
}

fun ObjectMapper.readTreeOrNull(body: String): JsonNode? = try {
    readTree(body)
} catch (e: Exception) {
    null
}
