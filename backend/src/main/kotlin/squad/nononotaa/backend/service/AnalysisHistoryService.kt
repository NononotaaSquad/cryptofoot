package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.dto.AnalysisResponse
import squad.nononotaa.backend.dto.PersonIdentifiersResponse
import squad.nononotaa.backend.dto.SourceCoverageResponse
import squad.nononotaa.backend.exception.NotFoundException
import squad.nononotaa.backend.model.AnalysisRecord
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.util.logger

@Service
class AnalysisHistoryService(
    private val repository: AnalysisRepository,
) {
    fun listByCase(caseId: String): List<AnalysisResponse> {
        val records = repository.findByCaseIdOrderByStartedAtDescIdDesc(caseId)
        logger.debug("Analysis history case={} count={}", caseId, records.size)
        return records.map(::toResponse)
    }

    fun get(analysisId: String): AnalysisResponse {
        val record =
            repository.findById(analysisId).orElseThrow {
                logger.warn("Analysis not found id={}", analysisId)
                NotFoundException("Nie znaleziono analizy o podanym identyfikatorze.")
            }
        return toResponse(record)
    }

    fun toResponse(record: AnalysisRecord): AnalysisResponse {
        val result = record.result
        val mentions = result?.mentions.orEmpty()
        return AnalysisResponse(
            id = requireNotNull(record.id) { "Stored analysis has no id" },
            caseId = record.caseId,
            trigger = record.trigger,
            status = record.status,
            startedAt = record.startedAt,
            finishedAt = record.finishedAt,
            identifiers = record.identifiers.toResponse(),
            sources =
            result?.sources.orEmpty().map { source ->
                SourceCoverageResponse(
                    sourceName = source.sourceName,
                    status = source.status,
                    synthetic = source.synthetic,
                    mentionCount = mentions.count { it.sourceName == source.sourceName },
                    checkedAt = source.checkedAt,
                )
            },
            networks = record.networks,
            algorithmVersion = result?.algorithmVersion,
            assessmentMode = result?.assessmentMode,
            modelVersion = result?.modelVersion,
            questionSetVersion = result?.questionSetVersion,
            attributionCount = result?.attributions?.size,
            excludedAddressCount = result?.excluded?.size,
            highestConfidenceLevel = result?.attributions?.maxOfOrNull { it.confidenceLevel },
        )
    }

    private fun List<CaseIdentifier>.toResponse() = PersonIdentifiersResponse(
        nickname = valueOf(IdentifierType.NICKNAME),
        login = valueOf(IdentifierType.LOGIN),
        email = valueOf(IdentifierType.EMAIL),
        messengerId = valueOf(IdentifierType.MESSENGER_ID),
        profileUrl = valueOf(IdentifierType.PROFILE_URL),
        fullName = valueOf(IdentifierType.FULL_NAME),
        walletAddress = valueOf(IdentifierType.WALLET_ADDRESS),
    )

    private fun List<CaseIdentifier>.valueOf(type: IdentifierType) = firstOrNull { it.type == type }?.value

    companion object {
        private val logger = logger()
    }
}
