package squad.nononotaa.backend.analysis.web2

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.MentionSource
import squad.nononotaa.backend.analysis.RawMention
import squad.nononotaa.backend.analysis.SourceNotCheckedException
import squad.nononotaa.backend.analysis.SourceUnavailableException
import squad.nononotaa.backend.config.Web2Properties
import squad.nononotaa.backend.util.logger
import java.net.URI
import java.time.Clock

/** Public profile (by numeric id from the URL, never from a nickname) and the posts of a given thread or post, each post under its own author. */
class BitcointalkMentionSource(
    private val properties: Web2Properties.Bitcointalk,
    private val clock: Clock,
) : MentionSource {
    override val sourceName = "bitcointalk"

    override val synthetic = false

    private val http = Web2Http(sourceName, properties.timeout, properties.maxBytes)

    private sealed interface Resource {
        data class Profile(
            val id: String,
        ) : Resource

        data class Topic(
            val topic: String,
            val position: String?,
            val message: String?,
        ) : Resource
    }

    override fun findMentions(identifiers: List<CaseIdentifier>): List<RawMention> {
        val resources =
            identifiers
                .filter { it.type == IdentifierType.PROFILE_URL }
                .mapNotNull { resource(it.value) }
                .distinct()
                .take(properties.maxResources)
        if (resources.isEmpty()) throw SourceNotCheckedException("no Bitcointalk profile or thread URL in the Case")
        return resources.flatMapIndexed { index, resource ->
            if (index > 0) pause()
            when (resource) {
                is Resource.Profile -> profile(resource)
                is Resource.Topic -> topic(resource)
            }
        }
    }

    private fun resource(value: String): Resource? {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return null
        if (uri.scheme !in setOf("http", "https") || uri.host?.lowercase() !in HOSTS || uri.path != "/index.php") return null
        val query = uri.rawQuery.orEmpty()
        if (ACTION_PROFILE.containsMatchIn(query)) USER_ID.find(query)?.let { return Resource.Profile(it.groupValues[1]) }
        val topic = TOPIC.find(query) ?: return null
        val message = topic.groups[2]?.value ?: MESSAGE.find(uri.rawFragment.orEmpty())?.groupValues?.get(1)
        return Resource.Topic(topic.groupValues[1], topic.groups[3]?.value, message)
    }

    private fun profile(resource: Resource.Profile): List<RawMention> {
        val page = fetch("/index.php?action=profile;u=${resource.id}")
        val name = field(page, "Name")?.text()?.trim().orEmpty()
        if (name.isEmpty()) return emptyIfMissing(page, PROFILE_MISSING)
        val address = field(page, "Bitcoin address")?.text()?.trim().orEmpty()
        val signature = (field(page, "Signature") ?: page.selectFirst("div.signature"))?.text()?.trim().orEmpty()
        val content = listOfNotNull(address.takeIf { it.isNotEmpty() }?.let { "Bitcoin address: $it" }, signature.takeIf { it.isNotEmpty() }?.let { "Signature: $it" })
        if (content.isEmpty()) return emptyList()
        return listOf(RawMention("$PUBLIC/index.php?action=profile;u=${resource.id}", clock.instant(), content.joinToString("\n"), name))
    }

    private fun topic(resource: Resource.Topic): List<RawMention> {
        val page = fetch("/index.php?topic=${resource.topic}.${resource.message?.let { "msg$it" } ?: resource.position ?: "0"}")
        val posts = page.select("div.post").mapNotNull { post(it) }
        if (posts.isEmpty()) return emptyIfMissing(page, TOPIC_MISSING)
        return posts.filter { resource.message == null || it.first == resource.message }.take(properties.maxPosts).map { (message, author, text) ->
            RawMention("$PUBLIC/index.php?topic=${resource.topic}.msg$message#msg$message", clock.instant(), text, author)
        }
    }

    private fun post(body: Element): Triple<String, String, String>? {
        val row = body.parents().firstOrNull { it.selectFirst("td.poster_info") != null } ?: return null
        val author = row.selectFirst("td.poster_info b a, td.poster_info b")?.text()?.trim().orEmpty()
        val message =
            POST_ID.find(body.id())?.groupValues?.get(1)
                ?: row.select("div.subject a[href]").firstNotNullOfOrNull { MESSAGE.find(it.attr("href"))?.groupValues?.get(1) }
        val copy = body.clone()
        copy.select("div.quote, div.quoteheader, div.quotefooter").remove()
        copy.select("br").after("\\n")
        val text = copy.text().replace("\\n", "\n").lines().joinToString("\n") { it.trim() }.trim()
        if (author.isEmpty() || message == null || text.isEmpty()) return null
        return Triple(message, author, text)
    }

    private fun field(
        page: Document,
        label: String,
    ): Element? = page
        .select("td")
        .firstOrNull { it.children().firstOrNull()?.takeIf { child -> child.tagName() == "b" }?.text()?.trim()?.trimEnd(':')?.trim().equals(label, ignoreCase = true) }
        ?.nextElementSibling()

    // an unrecognised 200 page (challenge, maintenance) must not look like "nothing there"
    private fun emptyIfMissing(
        page: Document,
        missing: Regex,
    ): List<RawMention> {
        if (missing.containsMatchIn(page.text())) {
            logger.info("Bitcointalk resource not found")
            return emptyList()
        }
        throw SourceUnavailableException("Bitcointalk answered with an unrecognised page")
    }

    private fun fetch(path: String): Document {
        val reply =
            http.send(
                http
                    .request(URI.create(properties.baseUrl.toString().trimEnd('/') + path))
                    .header("User-Agent", properties.userAgent)
                    .header("Accept", "text/html")
                    .GET()
                    .build(),
            )
        if (reply.status != 200) {
            logger.warn("Bitcointalk answered HTTP {}", reply.status)
            http.unavailable(reply)
        }
        return Jsoup.parse(reply.body)
    }

    private fun pause() {
        if (properties.requestDelay.isZero) return
        try {
            Thread.sleep(properties.requestDelay)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw SourceUnavailableException("Bitcointalk request interrupted", e)
        }
    }

    companion object {
        private const val PUBLIC = "https://bitcointalk.org"
        private val HOSTS = setOf("bitcointalk.org", "www.bitcointalk.org")
        private val ACTION_PROFILE = Regex("(?:^|[;&])action=profile(?=[;&]|$)")
        private val USER_ID = Regex("(?:^|[;&])u=(\\d+)(?=[;&]|$)")
        private val TOPIC = Regex("(?:^|[;&])topic=(\\d+)(?:\\.(?:msg(\\d+)|(\\d+)))?(?=[;&]|$)")
        private val MESSAGE = Regex("msg_?(\\d+)")
        private val POST_ID = Regex("^msg_(\\d+)$")
        private val PROFILE_MISSING = Regex("does not exist|doesn't exist", RegexOption.IGNORE_CASE)
        private val TOPIC_MISSING = Regex("missing or off limits", RegexOption.IGNORE_CASE)
        private val logger = logger()
    }
}
