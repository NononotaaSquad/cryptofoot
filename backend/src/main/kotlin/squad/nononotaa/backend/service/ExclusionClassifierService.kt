package squad.nononotaa.backend.service

import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.ExcludedCandidate
import squad.nononotaa.backend.analysis.ExclusionClassifier
import squad.nononotaa.backend.analysis.ExclusionResult
import squad.nononotaa.backend.model.ContractCodeCheck
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.util.logger

/**
 * Classifies address candidates as Excluded Addresses (issue #22, decisions D22-001 to D22-007).
 *
 * 1. Offline label lists (exchange, mixer, fundraiser). A hit decides and the address never leaves the application.
 * 2. ETH only: bytecode via [contractCodeSources] in order; the first usable answer decides. Code means SMART_CONTRACT.
 * 3. No usable answer, or the on-chain check is switched off: the candidate is unclassified, never accepted or excluded.
 *
 * A BTC address not on a list is accepted: the lists are always available and BTC has no contract code to check.
 */
class ExclusionClassifierService(
    private val labels: ExclusionLabelDirectory,
    private val contractCodeSources: List<ContractCodeSource>,
) : ExclusionClassifier {
    override fun classify(candidates: List<Candidate>): ExclusionResult {
        val accepted = mutableListOf<Candidate>()
        val excluded = mutableListOf<ExcludedCandidate>()
        val unclassified = mutableListOf<Candidate>()
        candidates.forEach { candidate ->
            when (val outcome = classify(candidate)) {
                Outcome.Accepted -> accepted += candidate
                Outcome.Unclassified -> unclassified += candidate
                is Outcome.Excluded -> excluded += ExcludedCandidate(candidate, outcome.reason, outcome.source)
            }
        }
        logger.info(
            "Classified {} candidates: accepted {}, excluded {} (reasons: {}), unclassified {}",
            candidates.size,
            accepted.size,
            excluded.size,
            excluded.groupingBy { it.reason }.eachCount(),
            unclassified.size,
        )
        return ExclusionResult(accepted, excluded, unclassified)
    }

    private fun classify(candidate: Candidate): Outcome {
        val network = Network.entries.firstOrNull { it.name == candidate.network }
        if (network == null) {
            logger.warn("Cannot classify a candidate of unsupported network {}", candidate.network)
            return Outcome.Unclassified
        }
        val address = ExclusionLabelDirectory.canonical(network, candidate.address)
        labels.find(address)?.let { return Outcome.Excluded(it.reason, "${it.source}: ${it.name}") }
        return when (network) {
            Network.BTC -> Outcome.Accepted
            Network.ETH -> if (ethAddress.matches(address.address)) checkContractCode(address.address) else malformed()
        }
    }

    /** Candidate preparation guarantees the format; anything else is never sent out and never guessed. */
    private fun malformed(): Outcome {
        logger.warn("Cannot check the contract code of a malformed ETH candidate")
        return Outcome.Unclassified
    }

    private fun checkContractCode(address: String): Outcome {
        for (source in contractCodeSources) {
            when (source.check(address)) {
                ContractCodeCheck.ACCOUNT -> return Outcome.Accepted
                ContractCodeCheck.CONTRACT -> return Outcome.Excluded(ExclusionReason.SMART_CONTRACT, "${source.sourceName}: executable code present")
                ContractCodeCheck.UNAVAILABLE -> continue
            }
        }
        return Outcome.Unclassified
    }

    private sealed interface Outcome {
        data object Accepted : Outcome

        data object Unclassified : Outcome

        data class Excluded(
            val reason: ExclusionReason,
            val source: String,
        ) : Outcome
    }

    companion object {
        private val logger = logger()
        private val ethAddress = Regex("^0x[0-9a-f]{40}$")
    }
}
