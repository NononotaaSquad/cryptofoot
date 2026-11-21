package squad.nononotaa.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

data class PersonIdentifiers(
    val nickname: String? = null,
    val login: String? = null,
    val email: String? = null,
    val messengerId: String? = null,
    val profileUrl: String? = null,
    val fullName: String? = null,
    val walletAddress: String? = null,
)

@Document("cases")
data class CaseRecord(
    @Id val id: String? = null,
    val identifiers: PersonIdentifiers,
    val createdAt: Instant,
)
