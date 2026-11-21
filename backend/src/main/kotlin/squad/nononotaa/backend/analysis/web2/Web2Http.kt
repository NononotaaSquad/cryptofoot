package squad.nononotaa.backend.analysis.web2

import squad.nononotaa.backend.analysis.SourceUnavailableException
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration

data class HttpReply(
    val status: Int,
    val body: String,
    val headers: Map<String, List<String>>,
) {
    fun header(name: String): String? = headers[name.lowercase()]?.firstOrNull()
}

class Web2Http(
    private val sourceName: String,
    private val timeout: Duration,
    private val maxBytes: Int,
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build(),
) {
    fun request(uri: java.net.URI): HttpRequest.Builder = HttpRequest.newBuilder(uri).timeout(timeout)

    fun send(request: HttpRequest): HttpReply = try {
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val body = response.body().use { String(it.readNBytes(maxBytes), Charsets.UTF_8) }
        HttpReply(response.statusCode(), body, response.headers().map().mapKeys { it.key.lowercase() })
    } catch (e: HttpTimeoutException) {
        throw SourceUnavailableException("$sourceName request timed out", e)
    } catch (e: IOException) {
        throw SourceUnavailableException("$sourceName request failed: ${e.javaClass.simpleName}", e)
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw SourceUnavailableException("$sourceName request interrupted", e)
    }

    fun unavailable(reply: HttpReply): Nothing = throw SourceUnavailableException("$sourceName answered HTTP ${reply.status}")
}
