package squad.nononotaa.backend.service

import squad.nononotaa.backend.analysis.AnalysisOrchestrator
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.dto.AnalysisResponse
import squad.nononotaa.backend.exception.ConflictException
import squad.nononotaa.backend.exception.NotFoundException
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.util.logger
import java.util.concurrent.Executor

interface CaseIdentifierSource {
    fun currentIdentifiers(caseId: String): List<CaseIdentifier>?
}

class AnalysisRerunService(
    private val orchestrator: AnalysisOrchestrator,
    private val repository: AnalysisRepository,
    private val identifiers: CaseIdentifierSource,
    private val history: AnalysisHistoryService,
    private val executor: Executor,
) {
    @JvmOverloads
    fun rerun(
        caseId: String,
        trigger: AnalysisTrigger = AnalysisTrigger.MANUAL,
    ): AnalysisResponse {
        val current =
            identifiers.currentIdentifiers(caseId) ?: run {
                logger.warn("Rerun rejected: case not found case={}", caseId)
                throw NotFoundException("Nie znaleziono sprawy o podanym identyfikatorze.")
            }
        if (repository.existsByCaseIdAndStatus(caseId, AnalysisStatus.RUNNING)) {
            logger.warn("Rerun rejected: an analysis is already running case={}", caseId)
            throw ConflictException("Analiza tej sprawy już trwa.")
        }
        val request = AnalysisRequest(caseId, trigger, current)
        val analysisId = orchestrator.start(request)
        executor.execute { execute(analysisId, request) }
        return history.get(analysisId)
    }

    private fun execute(
        analysisId: String,
        request: AnalysisRequest,
    ) {
        try {
            orchestrator.execute(analysisId, request)
        } catch (e: Exception) {
            logger.error("Analysis execution could not be recorded id={}", analysisId, e)
        }
    }

    companion object {
        private val logger = logger()
    }
}
