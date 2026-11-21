package squad.nononotaa.backend.analysis

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import squad.nononotaa.backend.model.Mention
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.MentionRepository
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
    @Value("\${sources.timeout:10s}") private val sourceTimeout: Duration,
) : MentionSearch {
    override fun search(identifiers: List<CaseIdentifier>): SearchResult {
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        try {
            val futures = sources.associateWith { executor.submit(Callable { query(it, identifiers) }) }
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
    ): Pair<SourceReport, List<RawMention>> = try {
        SourceReport(source.sourceName, SourceStatus.CHECKED, source.synthetic, Instant.now()) to source.findMentions(identifiers)
    } catch (e: Exception) {
        logger.warn("Source outcome source={} outcome=ERROR", source.sourceName)
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
        val match = findMatch(raw.content, identifiers) ?: return null
        val context = snippet(raw.content, match)
        val detectedAddresses = detector.detect("", context).map { it.rawAddress }
        val identity = Mention.computeIdentity(source.sourceName, raw.url, context)
        if (!seenIdentities.add(identity)) return null
        mentionRepository.findByIdentity(identity).firstOrNull()?.let { return it }
        return mentionRepository.save(Mention.create(source.sourceName, raw.url, raw.collectedAt, context, detectedAddresses))
    }

    private fun findMatch(
        content: String,
        identifiers: List<CaseIdentifier>,
    ): Match? {
        for (identifier in identifiers) {
            val value = identifier.value.trim()
            if (value.isEmpty()) continue
            if (isAddress(value)) {
                val index = content.indexOf(value)
                if (index >= 0 && detector.detect("", content).any { it.rawAddress == value }) return Match(index, value.length)
            } else {
                val index = content.indexOf(value, ignoreCase = true)
                if (index >= 0) return Match(index, value.length)
            }
        }
        return null
    }

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
