package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.dto.AnalysisComparisonResponse
import squad.nononotaa.backend.exception.BadRequestException
import squad.nononotaa.backend.exception.ConflictException
import squad.nononotaa.backend.exception.NotFoundException
import squad.nononotaa.backend.model.AnalysisRecord
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.util.logger

@Service
class AnalysisComparisonService(
    private val repository: AnalysisRepository,
    private val history: AnalysisHistoryService,
    private val comparer: AnalysisComparer,
) {
    fun compare(
        caseId: String,
        earlierAnalysisId: String,
        laterAnalysisId: String,
    ): AnalysisComparisonResponse {
        val earlier = load(earlierAnalysisId)
        val later = load(laterAnalysisId)
        if (earlier.caseId != caseId || later.caseId != caseId) {
            logger.warn("Comparison rejected: analyses {} and {} are not both of case {}", earlierAnalysisId, laterAnalysisId, caseId)
            throw BadRequestException("Porównać można tylko analizy tej samej sprawy.")
        }
        if (earlier.status == AnalysisStatus.RUNNING || later.status == AnalysisStatus.RUNNING) {
            logger.warn("Comparison rejected: an analysis is still running case={}", caseId)
            throw ConflictException("Nie można porównać analizy, która jeszcze trwa.")
        }
        if (earlier.startedAt.isAfter(later.startedAt)) {
            logger.warn("Comparison rejected: earlier analysis {} started after {}", earlierAnalysisId, laterAnalysisId)
            throw BadRequestException("Analiza wcześniejsza musi rozpocząć się przed późniejszą.")
        }
        val changes = comparer.changes(earlier, later)
        logger.debug("Compared analyses {} and {} changes={}", earlierAnalysisId, laterAnalysisId, changes.size)
        return AnalysisComparisonResponse(
            earlierAnalysis = history.toResponse(earlier),
            laterAnalysis = history.toResponse(later),
            changes = changes,
            comparabilityLimitations = comparer.limitations(earlier, later),
        )
    }

    private fun load(analysisId: String): AnalysisRecord = repository.findById(analysisId).orElseThrow {
        logger.warn("Analysis not found id={}", analysisId)
        NotFoundException("Nie znaleziono analizy o podanym identyfikatorze.")
    }

    companion object {
        private val logger = logger()
    }
}
