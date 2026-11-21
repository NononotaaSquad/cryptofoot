package squad.nononotaa.backend.service

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.dto.CaseResponse
import squad.nononotaa.backend.dto.CreateCaseRequest
import squad.nononotaa.backend.dto.PersonIdentifiersResponse
import squad.nononotaa.backend.exception.NotFoundException
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.CaseRecord
import squad.nononotaa.backend.model.PersonIdentifiers
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.repository.CaseRepository
import squad.nononotaa.backend.util.logger
import java.time.Clock

@Service
class CaseService(
    private val repository: CaseRepository,
    private val addressValidator: AddressValidator,
    private val clock: Clock,
    private val analyses: AnalysisRepository,
    private val history: AnalysisHistoryService,
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
                walletAddress = request.walletAddress?.let(::canonical),
            )
        val saved = repository.insert(CaseRecord(identifiers = identifiers, createdAt = clock.instant()))
        val id = requireNotNull(saved.id) { "Inserted case has no id" }
        logger.info("Case created id={} identifierTypes={}", id, saved.identifiers.toIdentifiers().map { it.type })
        return CaseResponse(id, saved.identifiers.toResponse(), saved.createdAt, null)
    }

    fun list(): List<CaseResponse> {
        val records = repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "_id"))
        logger.debug("Cases listed count={}", records.size)
        return records.map(::toResponse)
    }

    fun get(caseId: String): CaseResponse = toResponse(
        repository.findById(caseId).orElseThrow {
            logger.warn("Case not found id={}", caseId)
            NotFoundException("Nie znaleziono sprawy o podanym identyfikatorze.")
        },
    )

    override fun currentIdentifiers(caseId: String): List<CaseIdentifier>? = repository.findById(caseId).map { it.identifiers.toIdentifiers() }.orElse(null)

    private fun toResponse(record: CaseRecord): CaseResponse {
        val id = requireNotNull(record.id) { "Stored case has no id" }
        val latest = analyses.findFirstByCaseIdOrderByStartedAtDescIdDesc(id)?.let(history::toResponse)
        return CaseResponse(id, record.identifiers.toResponse(), record.createdAt, latest)
    }

    private fun canonical(rawAddress: String): String = when (val result = addressValidator.validate(rawAddress)) {
        is AddressValidation.Valid -> result.canonicalAddress.address
        is AddressValidation.Invalid -> error("Wallet address passed request validation but is invalid")
    }

    private fun PersonIdentifiers.toResponse() = PersonIdentifiersResponse(nickname, login, email, messengerId, profileUrl, fullName, walletAddress)

    private fun PersonIdentifiers.toIdentifiers() = listOfNotNull(
        nickname?.let { CaseIdentifier(IdentifierType.NICKNAME, it) },
        login?.let { CaseIdentifier(IdentifierType.LOGIN, it) },
        email?.let { CaseIdentifier(IdentifierType.EMAIL, it) },
        messengerId?.let { CaseIdentifier(IdentifierType.MESSENGER_ID, it) },
        profileUrl?.let { CaseIdentifier(IdentifierType.PROFILE_URL, it) },
        walletAddress?.let { CaseIdentifier(IdentifierType.WALLET_ADDRESS, it) },
        fullName?.let { CaseIdentifier(IdentifierType.FULL_NAME, it) },
    )

    companion object {
        private val logger = logger()
    }
}
