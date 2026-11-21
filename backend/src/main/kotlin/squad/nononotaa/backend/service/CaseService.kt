package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.dto.CaseResponse
import squad.nononotaa.backend.dto.CreateCaseRequest
import squad.nononotaa.backend.dto.PersonIdentifiersResponse
import squad.nononotaa.backend.model.CaseRecord
import squad.nononotaa.backend.model.PersonIdentifiers
import squad.nononotaa.backend.repository.CaseRepository
import squad.nononotaa.backend.util.logger
import java.time.Clock

@Service
class CaseService(
    private val repository: CaseRepository,
    private val clock: Clock,
) : CaseIdentifierSource {
    fun create(request: CreateCaseRequest): CaseResponse {
        val identifiers =
            PersonIdentifiers(
                nickname = request.nickname,
                login = request.login,
                email = request.email,
                messengerId = request.messengerId,
                profileUrl = request.profileUrl,
                fullName = request.fullName,
            )
        val saved = repository.insert(CaseRecord(identifiers = identifiers, createdAt = clock.instant()))
        val id = requireNotNull(saved.id) { "Inserted case has no id" }
        logger.info("Case created id={} identifierTypes={}", id, saved.identifiers.toIdentifiers().map { it.type })
        return CaseResponse(id, saved.identifiers.toResponse(), saved.createdAt, null)
    }

    override fun currentIdentifiers(caseId: String): List<CaseIdentifier>? = repository.findById(caseId).map { it.identifiers.toIdentifiers() }.orElse(null)

    private fun PersonIdentifiers.toResponse() = PersonIdentifiersResponse(nickname, login, email, messengerId, profileUrl, fullName)

    private fun PersonIdentifiers.toIdentifiers() = listOfNotNull(
        nickname?.let { CaseIdentifier(IdentifierType.NICKNAME, it) },
        login?.let { CaseIdentifier(IdentifierType.LOGIN, it) },
        email?.let { CaseIdentifier(IdentifierType.EMAIL, it) },
        messengerId?.let { CaseIdentifier(IdentifierType.MESSENGER_ID, it) },
        profileUrl?.let { CaseIdentifier(IdentifierType.PROFILE_URL, it) },
        fullName?.let { CaseIdentifier(IdentifierType.FULL_NAME, it) },
    )

    companion object {
        private val logger = logger()
    }
}
