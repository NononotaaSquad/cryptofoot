package squad.nononotaa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import squad.nononotaa.backend.model.AnalysisRecord
import squad.nononotaa.backend.model.AnalysisStatus

interface AnalysisRepository : MongoRepository<AnalysisRecord, String> {
    fun findByCaseIdOrderByStartedAtDescIdDesc(caseId: String): List<AnalysisRecord>

    fun findFirstByCaseIdOrderByStartedAtDescIdDesc(caseId: String): AnalysisRecord?

    fun findFirstByCaseIdAndStatusInOrderByStartedAtDescIdDesc(
        caseId: String,
        statuses: Collection<AnalysisStatus>,
    ): AnalysisRecord?

    fun existsByCaseIdAndStatus(
        caseId: String,
        status: AnalysisStatus,
    ): Boolean
}
