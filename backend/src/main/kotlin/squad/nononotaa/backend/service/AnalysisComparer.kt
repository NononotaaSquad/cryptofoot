package squad.nononotaa.backend.service

import org.springframework.stereotype.Component
import squad.nononotaa.backend.analysis.ExcludedCandidate
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.dto.AttributionChangeResponse
import squad.nononotaa.backend.model.AnalysisRecord
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AttributionChangeType
import squad.nononotaa.backend.model.ComparabilityLimitation
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.model.SourceStatus

@Component
class AnalysisComparer {
    fun changes(
        earlier: AnalysisRecord,
        later: AnalysisRecord,
    ): List<AttributionChangeResponse> {
        val before = earlier.sides()
        val after = later.sides()
        return (before.keys + after.keys)
            .sortedWith(compareBy({ it.first }, { it.second }))
            .mapNotNull { key -> change(key, before[key], after[key], earlier, later) }
    }

    fun limitations(
        earlier: AnalysisRecord,
        later: AnalysisRecord,
    ): List<ComparabilityLimitation> = listOfNotNull(
        ComparabilityLimitation.IDENTIFIERS_DIFFER.takeIf { earlier.identifiers.toSet() != later.identifiers.toSet() },
        ComparabilityLimitation.SOURCE_COVERAGE_DIFFERS.takeIf { earlier.sourceStatuses() != later.sourceStatuses() },
        ComparabilityLimitation.NETWORKS_DIFFER.takeIf { earlier.networks.toSet() != later.networks.toSet() },
        ComparabilityLimitation.ALGORITHM_VERSION_DIFFERS.takeIf { earlier.result?.algorithmVersion != later.result?.algorithmVersion },
        ComparabilityLimitation.PARTIAL_ANALYSIS.takeIf {
            earlier.status != AnalysisStatus.COMPLETED || later.status != AnalysisStatus.COMPLETED
        },
    )

    private fun change(
        key: Pair<String, String>,
        before: Side?,
        after: Side?,
        earlier: AnalysisRecord,
        later: AnalysisRecord,
    ): AttributionChangeResponse? {
        val type =
            when {
                before is Side.Attributed && after == null -> AttributionChangeType.NOT_OBSERVED

                before == null && after is Side.Attributed -> AttributionChangeType.NEW

                before is Side.Attributed && after is Side.Attributed ->
                    AttributionChangeType.CONFIDENCE_LEVEL_CHANGED.takeIf { before.attribution.confidenceLevel != after.attribution.confidenceLevel }

                before is Side.Excluded && after is Side.Excluded ->
                    AttributionChangeType.EXCLUSION_CHANGED.takeIf { before.excluded.reason != after.excluded.reason }

                before != null && after != null -> AttributionChangeType.EXCLUSION_CHANGED

                else -> null
            } ?: return null
        return AttributionChangeResponse(
            address = key.second,
            network = networkOf(key.first),
            change = type,
            earlierConfidenceLevel = (before as? Side.Attributed)?.attribution?.confidenceLevel,
            laterConfidenceLevel = (after as? Side.Attributed)?.attribution?.confidenceLevel,
            earlierExclusionReason = (before as? Side.Excluded)?.excluded?.reason,
            laterExclusionReason = (after as? Side.Excluded)?.excluded?.reason,
            earlierMentionIds = before?.mentionIds.orEmpty(),
            laterMentionIds = after?.mentionIds.orEmpty(),
            absenceVerifiable = if (type == AttributionChangeType.NOT_OBSERVED) absenceVerifiable(key, before, earlier, later) else null,
        )
    }

    private fun absenceVerifiable(
        key: Pair<String, String>,
        before: Side?,
        earlier: AnalysisRecord,
        later: AnalysisRecord,
    ): Boolean {
        val supportingSources =
            earlier.result
                ?.mentions
                .orEmpty()
                .filter { it.id in before?.mentionIds.orEmpty() }
                .map { it.sourceName }
                .toSet()
        val laterSources = later.sourceStatuses()
        return later.status == AnalysisStatus.COMPLETED &&
            networkOf(key.first) in later.networks &&
            supportingSources.isNotEmpty() &&
            supportingSources.all { laterSources[it] == SourceStatus.CHECKED }
    }

    private fun networkOf(name: String) = Network.entries.firstOrNull { it.name == name } ?: error("Unsupported network in stored analysis: $name")

    private fun AnalysisRecord.sourceStatuses() = result?.sources.orEmpty().associate { it.sourceName to it.status }

    private fun AnalysisRecord.sides(): Map<Pair<String, String>, Side> {
        val attributed = result?.attributions.orEmpty().associate { (it.candidate.network to it.candidate.address) to Side.Attributed(it) }
        val excluded = result?.excluded.orEmpty().associate { (it.candidate.network to it.candidate.address) to Side.Excluded(it) }
        return attributed + excluded
    }

    private sealed interface Side {
        val mentionIds: List<String>

        data class Attributed(
            val attribution: ScoredAttribution,
        ) : Side {
            override val mentionIds get() = attribution.candidate.mentionIds
        }

        data class Excluded(
            val excluded: ExcludedCandidate,
        ) : Side {
            override val mentionIds get() = excluded.candidate.mentionIds
        }
    }
}
