package squad.nononotaa.backend.service

import squad.nononotaa.backend.analysis.AddressActivityCollector
import squad.nononotaa.backend.analysis.AddressActivityOutcome
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.model.AddressActivityStatus
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.util.logger
import java.time.Instant

// Only Attributions with a Confidence Level above 0 are checked (D23-002); the first source with an answer decides (D23-003)
class AddressActivityService(
    private val sources: List<AddressActivitySource>,
    private val evaluator: AddressActivityEvaluator,
) : AddressActivityCollector {
    override fun collect(attributions: List<ScoredAttribution>): Map<Candidate, AddressActivityOutcome> {
        val outcomes = attributions.filter { it.confidenceLevel > 0 }.associate { it.candidate to outcome(it.candidate) }
        logger.info(
            "Address Activity checked for {} of {} Attributions: {}",
            outcomes.size,
            attributions.size,
            outcomes.values.groupingBy { it.status }.eachCount(),
        )
        return outcomes
    }

    private fun outcome(candidate: Candidate): AddressActivityOutcome {
        val network = Network.entries.firstOrNull { it.name == candidate.network }
        if (network == null) {
            logger.warn("No Address Activity source for unsupported network {}", candidate.network)
            return unavailable
        }
        val address = ExclusionLabelDirectory.canonical(network, candidate.address)
        for (source in sources.filter { it.covers(address) }) {
            val chain = fetch(source, address) ?: continue
            logger.debug("Address Activity read from {} transfers={} complete={}", source.sourceName, chain.transfers.size, chain.complete)
            return AddressActivityOutcome(AddressActivityStatus.AVAILABLE, evaluator.evaluate(address, source, chain, Instant.now()))
        }
        logger.warn("Address Activity unavailable for a {} address: no source answered", network)
        return unavailable
    }

    private fun fetch(
        source: AddressActivitySource,
        address: CanonicalAddress,
    ): ChainActivity? = try {
        source.fetch(address)
    } catch (e: Exception) {
        logger.warn("Address Activity source {} failed: {}", source.sourceName, e.toString())
        null
    }

    companion object {
        private val logger = logger()
        private val unavailable = AddressActivityOutcome(AddressActivityStatus.UNAVAILABLE)
    }
}
