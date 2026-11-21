package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.model.AddressCandidate
import squad.nononotaa.backend.model.AddressCandidates
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.RawAddressCandidate
import squad.nononotaa.backend.model.RejectedAddressCandidate
import squad.nononotaa.backend.util.logger

@Service
class AddressCandidateService(
    private val addressValidator: AddressValidator,
) {
    // TODO E2.1/E3.1: call from the Analysis pipeline between Mention collection and Confidence Level calculation
    fun prepare(rawCandidates: List<RawAddressCandidate>): AddressCandidates {
        logger.debug("Preparing address candidates from {} raw candidates", rawCandidates.size)
        val mentionIdsByAddress = mutableMapOf<CanonicalAddress, MutableSet<String>>()
        val rejected = mutableListOf<RejectedAddressCandidate>()
        rawCandidates.forEach { raw ->
            when (val validation = addressValidator.validate(raw.rawAddress, raw.networkHint)) {
                is AddressValidation.Valid -> mentionIdsByAddress.getOrPut(validation.canonicalAddress) { sortedSetOf() } += raw.mentionId
                is AddressValidation.Invalid -> rejected += RejectedAddressCandidate(raw.mentionId, raw.rawAddress, validation.failure)
            }
        }
        val candidates =
            mentionIdsByAddress
                .map { (address, mentionIds) -> AddressCandidate(address, mentionIds.toList()) }
                .sortedWith(compareBy({ it.canonicalAddress.network }, { it.canonicalAddress.address }))
        logger.info(
            "Prepared {} address candidates, rejected {} raw candidates (failures: {})",
            candidates.size,
            rejected.size,
            rejected.groupingBy { it.failure }.eachCount(),
        )
        return AddressCandidates(candidates, rejected)
    }

    companion object {
        private val logger = logger()
    }
}
