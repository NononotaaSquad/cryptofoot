package squad.nononotaa.backend.analysis

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.Mention
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.MentionRepository
import squad.nononotaa.backend.service.AddressValidator
import squad.nononotaa.backend.util.logger
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class SearchService(
    private val sources: List<MentionSource>,
    private val detector: AddressCandidateDetector,
    private val mentionRepository: MentionRepository,
    private val addressValidator: AddressValidator,
    @Value("\${sources.timeout:10s}") private val sourceTimeout: Duration,
) : MentionSearch {
    override fun search(
        identifiers: List<CaseIdentifier>,
        earlierAnalyses: Int,
    ): SearchResult {
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        try {
            val futures = sources.associateWith { executor.submit(Callable { query(it, identifiers, earlierAnalyses) }) }
            val reports = mutableListOf<SourceReport>()
            val mentions = mutableListOf<FoundMention>()
            val seenIdentities = mutableSetOf<String>()
            for ((source, future) in futures) {
                val (report, rawMentions) = await(source, future)
                reports += report
                for (raw in rawMentions) {
                    toMention(source, raw, identifiers, seenIdentities)?.let { mentions += it.toFoundMention() }
                }
            }
            val unavailable = reports.count { it.status == SourceStatus.UNAVAILABLE }
            logger.info("Search finished sources={} unavailable={} mentions={}", reports.size, unavailable, mentions.size)
            return SearchResult(mentions, reports)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun query(
        source: MentionSource,
        identifiers: List<CaseIdentifier>,
        earlierAnalyses: Int,
    ): Pair<SourceReport, List<RawMention>> = try {
        SourceReport(source.sourceName, SourceStatus.CHECKED, source.synthetic, Instant.now()) to source.findMentions(identifiers, earlierAnalyses)
    } catch (e: SourceNotCheckedException) {
        logger.info("Source outcome source={} outcome=NOT_CHECKED reason={}", source.sourceName, e.message)
        SourceReport(source.sourceName, SourceStatus.NOT_CHECKED, source.synthetic, null) to emptyList()
    } catch (e: Exception) {
        logger.warn("Source outcome source={} outcome=ERROR reason={}", source.sourceName, e.message)
        unavailable(source) to emptyList()
    }

    private fun await(
        source: MentionSource,
        future: Future<Pair<SourceReport, List<RawMention>>>,
    ): Pair<SourceReport, List<RawMention>> = try {
        future.get(sourceTimeout.toMillis(), TimeUnit.MILLISECONDS)
    } catch (e: TimeoutException) {
        future.cancel(true)
        logger.warn("Source outcome source={} outcome=TIMEOUT", source.sourceName)
        unavailable(source) to emptyList()
    } catch (e: Exception) {
        future.cancel(true)
        logger.warn("Source outcome source={} outcome=ERROR", source.sourceName)
        unavailable(source) to emptyList()
    }

    private fun unavailable(source: MentionSource) = SourceReport(source.sourceName, SourceStatus.UNAVAILABLE, source.synthetic, null)

    private fun toMention(
        source: MentionSource,
        raw: RawMention,
        identifiers: List<CaseIdentifier>,
        seenIdentities: MutableSet<String>,
    ): Mention? {
        val match = findMatch(raw.content, identifiers) ?: findByProvenance(raw, identifiers) ?: return null
        val context = snippet(raw.content, match)
        val detectedAddresses = detector.detect("", context).map { it.rawAddress }
        val identity = Mention.computeIdentity(source.sourceName, raw.url, context)
        if (!seenIdentities.add(identity)) return null
        mentionRepository.findByIdentity(identity).firstOrNull()?.let { return it }
        return mentionRepository.save(Mention.create(source.sourceName, raw.url, raw.collectedAt, context, detectedAddresses, raw.author))
    }

    private fun findMatch(
        content: String,
        identifiers: List<CaseIdentifier>,
    ): Match? {
        for (identifier in identifiers) {
            // a full name alone is too general to make a Mention relevant (E1.1)
            if (identifier.type == IdentifierType.FULL_NAME) continue
            val value = identifier.value.trim()
            if (value.isEmpty()) continue
            val match = if (isAddress(value)) findAddress(content, value) else findText(content, value)
            if (match != null) return match
        }
        return null
    }

    // the source fetched this resource for the identifier (author or profile URL); only an address it publishes is evidence
    private fun findByProvenance(
        raw: RawMention,
        identifiers: List<CaseIdentifier>,
    ): Match? {
        val owned =
            identifiers.any {
                val value = it.value.trim()
                when (it.type) {
                    IdentifierType.NICKNAME, IdentifierType.LOGIN -> value.isNotEmpty() && raw.author.equals(value.removePrefix("@"), ignoreCase = true)
                    IdentifierType.PROFILE_URL -> value.isNotEmpty() && isUnder(raw.url, value)
                    else -> false
                }
            }
        if (!owned) return null
        return detector.detect("", raw.content).firstOrNull()?.let { Match(raw.content.indexOf(it.rawAddress), it.rawAddress.length) }
    }

    private fun isUnder(
        url: String,
        profile: String,
    ): Boolean {
        val page = bare(url)
        val prefix = bare(profile)
        return page == prefix || listOf('/', '?', '#').any { page.startsWith("$prefix$it") }
    }

    private fun bare(url: String) = url
        .trim()
        .lowercase()
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')

    private fun findText(
        content: String,
        value: String,
    ): Match? = content.indexOf(value, ignoreCase = true).takeIf { it >= 0 }?.let { Match(it, value.length) }

    private fun findAddress(
        content: String,
        value: String,
    ): Match? = detector
        .detect("", content)
        .firstOrNull { sameAddress(it.rawAddress, value) }
        ?.let { Match(content.indexOf(it.rawAddress), it.rawAddress.length) }

    // the Case stores the canonical form (ETH lowercased), sources keep the original spelling (D50-002)
    private fun sameAddress(
        found: String,
        wanted: String,
    ): Boolean {
        val foundCanonical = canonical(found)
        val wantedCanonical = canonical(wanted)
        return if (foundCanonical != null && wantedCanonical != null) foundCanonical == wantedCanonical else found == wanted
    }

    private fun canonical(address: String) = (addressValidator.validate(address) as? AddressValidation.Valid)?.canonicalAddress

    private fun isAddress(value: String): Boolean = detector.detect("", value).isNotEmpty()

    private fun snippet(
        content: String,
        match: Match,
    ): String {
        val start = maxOf(0, match.index - SNIPPET_MARGIN)
        val end = minOf(content.length, start + MAX_SNIPPET_LENGTH)
        return content.substring(start, end)
    }

    private data class Match(
        val index: Int,
        val length: Int,
    )

    companion object {
        private const val MAX_SNIPPET_LENGTH = 200
        private const val SNIPPET_MARGIN = 100
        private val logger = logger()
    }
}
