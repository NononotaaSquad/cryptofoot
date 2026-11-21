package squad.nononotaa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import squad.nononotaa.backend.model.AnalysisRecord

interface AnalysisRepository : MongoRepository<AnalysisRecord, String> {
    fun findByCaseIdOrderByStartedAtDescIdDesc(caseId: String): List<AnalysisRecord>
}
