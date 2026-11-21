package squad.nononotaa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import squad.nononotaa.backend.model.ScheduleRecord
import java.time.Instant

interface ScheduleRepository : MongoRepository<ScheduleRecord, String> {
    fun findByEnabledTrueAndNextExecutionAtLessThanEqual(now: Instant): List<ScheduleRecord>
}
