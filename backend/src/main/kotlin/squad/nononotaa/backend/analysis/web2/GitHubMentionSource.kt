package squad.nononotaa.backend.analysis.web2

import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.MentionSource
import squad.nononotaa.backend.analysis.RawMention
import squad.nononotaa.backend.analysis.SourceNotCheckedException
import squad.nononotaa.backend.analysis.SourceUnavailableException
import squad.nononotaa.backend.config.Web2Properties
import squad.nononotaa.backend.util.logger
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.time.Clock

/** Public profile (REST API `/users/{login}`) and the profile README of `{login}/{login}`; one canonical github.com URL per resource. */
class GitHubMentionSource(
    private val properties: Web2Properties.GitHub,
    private val mapper: ObjectMapper,
    private val clock: Clock,
) : MentionSource {
    override val sourceName = "github"

    override val synthetic = false

    private val http = Web2Http(sourceName, properties.timeout, properties.maxBytes)

    override fun findMentions(identifiers: List<CaseIdentifier>): List<RawMention> {
        val logins = Web2Identifiers.handles(identifiers, HOSTS, { it.singleOrNull() }, LOGIN, properties.maxLogins)
        if (logins.isEmpty()) throw SourceNotCheckedException("no GitHub login or profile URL in the Case")
        return logins.flatMap(::fetch)
    }

    private fun fetch(login: String): List<RawMention> {
        val profile = get("/users/$login", "application/vnd.github+json")
        if (profile.status == 404) {
            logger.info("GitHub profile not found")
            return emptyList()
        }
        val user = parse(profile)
        val canonical = user.path("login").asString("").takeIf { LOGIN.matches(it) } ?: throw SourceUnavailableException("GitHub profile without a login")
        val mentions = mutableListOf<RawMention>()
        val bio = listOf("name", "bio", "company", "location", "blog").map { user.path(it).asString("").trim() }.filter { it.isNotEmpty() }
        if (bio.isNotEmpty()) mentions += RawMention("https://github.com/$canonical", clock.instant(), bio.joinToString("\n"), canonical)

        val readme = get("/repos/$canonical/$canonical/readme", "application/vnd.github.raw+json")
        if (readme.status == 404) {
            logger.info("GitHub profile README not found")
        } else if (readme.body.isNotBlank()) {
            mentions += RawMention("https://github.com/$canonical/$canonical", clock.instant(), readme.body, canonical)
        }
        return mentions
    }

    private fun get(
        path: String,
        accept: String,
    ): HttpReply {
        val builder =
            http
                .request(URI.create(properties.apiUrl.toString().trimEnd('/') + path))
                .header("Accept", accept)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "cryptofoot-backend")
        if (properties.token.isNotBlank()) builder.header("Authorization", "Bearer ${properties.token}")
        val reply = http.send(builder.GET().build())
        if (reply.status == 200 || (reply.status == 404 && reply.header("x-ratelimit-remaining") != "0")) return reply
        logger.warn("GitHub answered HTTP {} rateLimitRemaining={}", reply.status, reply.header("x-ratelimit-remaining"))
        http.unavailable(reply)
    }

    private fun parse(reply: HttpReply): JsonNode = try {
        mapper.readTree(reply.body)
    } catch (e: RuntimeException) {
        throw SourceUnavailableException("GitHub answered with unreadable JSON", e)
    }

    companion object {
        private val HOSTS = setOf("github.com", "www.github.com")
        private val LOGIN = Regex("[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})")
        private val logger = logger()
    }
}
