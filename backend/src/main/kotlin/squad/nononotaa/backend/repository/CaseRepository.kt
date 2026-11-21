package squad.nononotaa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import squad.nononotaa.backend.model.CaseRecord

interface CaseRepository : MongoRepository<CaseRecord, String>
