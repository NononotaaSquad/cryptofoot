package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CandidatePreparation
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.PreparedCandidates
import squad.nononotaa.backend.analysis.RejectedCandidate
import squad.nononotaa.backend.model.RawAddressCandidate
import squad.nononotaa.backend.util.logger

@Service
class CandidatePreparationService(
    private val addressCandidates: AddressCandidateService,
) : CandidatePreparation {
    override fun prepare(mentions: List<FoundMention>): PreparedCandidates {
        val raw = mentions.flatMap { mention -> mention.detectedAddresses.map { RawAddressCandidate(mention.id, it) } }
        logger.debug("Preparing candidates from {} mentions and {} detected addresses", mentions.size, raw.size)
        val prepared = addressCandidates.prepare(raw)
        return PreparedCandidates(
            valid =
            prepared.candidates.map {
                Candidate(it.canonicalAddress.network.name, it.canonicalAddress.address, it.mentionIds)
            },
            rejected = prepared.rejected.map { RejectedCandidate(it.mentionId, it.rawAddress, it.failure.name) },
        )
    }

    companion object {
        private val logger = logger()
    }
}
