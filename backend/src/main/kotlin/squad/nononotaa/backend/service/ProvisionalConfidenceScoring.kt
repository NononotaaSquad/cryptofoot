package squad.nononotaa.backend.service

import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.ConfidenceScoring
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.ScoringResult
import squad.nononotaa.backend.util.logger
import java.net.URI

// Placeholder for the E3 algorithm of issue #9 (decision DS1-003): replace this bean, keep the ConfidenceScoring port.
class ProvisionalConfidenceScoring : ConfidenceScoring {
    override fun score(
        candidates: List<Candidate>,
        mentions: List<FoundMention>,
    ): ScoringResult {
        val byId = mentions.associateBy { it.id }
        val attributions =
            candidates.map { candidate ->
                val supporting = candidate.mentionIds.mapNotNull(byId::get)
                val rules =
                    listOfNotNull(
                        AppliedRule(MENTIONED_WITH_IDENTIFIER, BASE),
                        AppliedRule(INDEPENDENT_SOURCES, INDEPENDENT_SOURCES_BONUS).takeIf { independentlyConfirmed(supporting) },
                    )
                ScoredAttribution(candidate, rules.sumOf { it.contribution }.coerceIn(0, MAX), rules)
            }
        logger.info("Scored {} candidates with algorithm {}", attributions.size, ALGORITHM_VERSION)
        return ScoringResult(attributions, ALGORITHM_VERSION)
    }

    // a copy of the same text on another service is not independent (E7.3), several Mentions on one service neither
    private fun independentlyConfirmed(mentions: List<FoundMention>) = mentions.any { first ->
        mentions.any { second -> origin(first) != origin(second) && text(first) != text(second) }
    }

    private fun origin(mention: FoundMention) = runCatching { URI(mention.url).host }
        .getOrNull()
        ?.lowercase()
        ?.removePrefix("www.")
        ?: mention.url

    private fun text(mention: FoundMention) = mention.context.orEmpty().trim().replace(WHITESPACE, " ").lowercase()

    companion object {
        const val ALGORITHM_VERSION = "provisional-1"
        const val MENTIONED_WITH_IDENTIFIER = "MENTIONED_WITH_IDENTIFIER"
        const val INDEPENDENT_SOURCES = "INDEPENDENT_SOURCES"
        const val BASE = 40
        const val INDEPENDENT_SOURCES_BONUS = 15
        private const val MAX = 100
        private val WHITESPACE = Regex("\\s+")
        private val logger = logger()
    }
}
