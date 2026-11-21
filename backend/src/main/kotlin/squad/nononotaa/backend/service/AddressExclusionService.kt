package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.model.AddressCandidate
import squad.nononotaa.backend.model.AddressClassifications
import squad.nononotaa.backend.model.ExcludedAddress
import squad.nononotaa.backend.util.logger

@Service
class AddressExclusionService(
    private val sources: List<AddressClassificationSource>,
) {
    // TODO E3.1: call from the Analysis pipeline after AddressCandidateService.prepare; only `accepted` and `unavailable`
    //  go to the Confidence Level algorithm (D24-006), `excluded` never does
    fun classify(candidates: List<AddressCandidate>): AddressClassifications {
        val answers = sources.map { source -> source to ask(source, candidates) }
        val checkedSources = answers.filter { (_, answer) -> answer != null }.map { (source, _) -> source.name }
        val unavailableSources = answers.filter { (_, answer) -> answer == null }.map { (source, _) -> source.name }

        val accepted = mutableListOf<AddressCandidate>()
        val excluded = mutableListOf<ExcludedAddress>()
        val unavailable = mutableListOf<AddressCandidate>()
        candidates.forEach { candidate ->
            val address = candidate.canonicalAddress
            val applicable = answers.filter { (source, _) -> address.network in source.networks }
            val match =
                applicable.firstNotNullOfOrNull { (source, answer) ->
                    answer?.get(address)?.let { known -> ExcludedAddress(candidate, known.reason, "${source.name}: ${known.label}") }
                }
            when {
                match != null -> excluded += match
                applicable.isEmpty() || applicable.any { (_, answer) -> answer == null } -> unavailable += candidate
                else -> accepted += candidate
            }
        }
        logger.info(
            "Classified {} candidates: {} accepted, {} excluded {}, {} unavailable (unavailable sources: {})",
            candidates.size,
            accepted.size,
            excluded.size,
            excluded.groupingBy { it.reason }.eachCount(),
            unavailable.size,
            unavailableSources,
        )
        return AddressClassifications(accepted, excluded, unavailable, checkedSources, unavailableSources)
    }

    /** Returns null when the source cannot answer; the message is not logged as it may carry addresses. */
    private fun ask(
        source: AddressClassificationSource,
        candidates: List<AddressCandidate>,
    ) = try {
        source.findExcluded(candidates.map { it.canonicalAddress }.filter { it.network in source.networks })
    } catch (e: ClassificationUnavailableException) {
        logger.warn("Classification source {} unavailable ({})", source.name, e.cause?.javaClass?.simpleName)
        null
    }

    companion object {
        private val logger = logger()
    }
}
