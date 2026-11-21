package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.AnalysisResult
import squad.nononotaa.backend.model.AnalysisRecord
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.util.logger
import java.time.Instant

@Service
class AnalysisRecorderService(
    private val repository: AnalysisRepository,
) : AnalysisRecorder {
    override fun begin(request: AnalysisRequest): String {
        val saved =
            repository.insert(
                AnalysisRecord(
                    caseId = request.caseId,
                    trigger = request.trigger,
                    status = AnalysisStatus.RUNNING,
                    startedAt = Instant.now(),
                    identifiers = request.identifiers,
                    networks = Network.entries,
                ),
            )
        val id = requireNotNull(saved.id) { "Inserted analysis has no id" }
        logger.info("Analysis recorded id={} case={} trigger={} status={}", id, request.caseId, request.trigger, saved.status)
        return id
    }

    override fun finish(
        analysisId: String,
        result: AnalysisResult,
    ) {
        check(result.status != AnalysisStatus.RUNNING) { "Finished analysis cannot be RUNNING id=$analysisId" }
        val record = repository.findById(analysisId).orElseThrow { IllegalStateException("Analysis not found id=$analysisId") }
        check(record.status == AnalysisStatus.RUNNING) { "Analysis already finished id=$analysisId" }
        repository.save(record.copy(status = result.status, finishedAt = Instant.now(), result = result))
        logger.info("Analysis result recorded id={} status={} failures={}", analysisId, result.status, result.failures.size)
    }

    companion object {
        private val logger = logger()
    }
}
