package squad.nononotaa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import squad.nononotaa.backend.model.Mention

interface MentionRepository : MongoRepository<Mention, String> {
    fun findByIdentity(identity: String): List<Mention>
}
