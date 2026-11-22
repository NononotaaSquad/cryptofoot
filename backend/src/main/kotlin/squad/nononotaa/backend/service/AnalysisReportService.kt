package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.Premise
import squad.nononotaa.backend.analysis.PremiseAnswers
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.YesNoProbabilities
import squad.nononotaa.backend.dto.AttributionResponse
import squad.nononotaa.backend.dto.DetectedAddressResponse
import squad.nononotaa.backend.dto.ExcludedAddressResponse
import squad.nononotaa.backend.dto.KindProbabilitiesResponse
import squad.nononotaa.backend.dto.MentionResponse
import squad.nononotaa.backend.dto.PremiseAnswersResponse
import squad.nononotaa.backend.dto.PremiseFactsResponse
import squad.nononotaa.backend.dto.PremiseResponse
import squad.nononotaa.backend.dto.ReportResponse
import squad.nononotaa.backend.dto.ScoringRuleContributionResponse
import squad.nononotaa.backend.dto.YesNoProbabilitiesResponse
import squad.nononotaa.backend.exception.ConflictException
import squad.nononotaa.backend.exception.NotFoundException
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.AnalysisRecord
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.Network
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.util.logger

@Service
class AnalysisReportService(
    private val repository: AnalysisRepository,
    private val history: AnalysisHistoryService,
    private val addressValidator: AddressValidator,
) {
    fun mentions(analysisId: String): List<MentionResponse> {
        val mentions = finished(analysisId).result?.mentions.orEmpty()
        logger.debug("Mentions read analysis={} count={}", analysisId, mentions.size)
        return mentions.map(::toResponse)
    }

    fun report(analysisId: String): ReportResponse {
        val record = finished(analysisId)
        val result = record.result
        val foundMentions = result?.mentions.orEmpty().associateBy { it.id }
        val mentions = foundMentions.mapValues { toResponse(it.value) }
        val attributions =
            result
                ?.attributions
                .orEmpty()
                .sortedWith(
                    compareByDescending<ScoredAttribution> { it.confidenceLevel }
                        .thenBy { it.candidate.network }
                        .thenBy { it.candidate.address },
                ).map {
                    AttributionResponse(
                        address = it.candidate.address,
                        network = Network.valueOf(it.candidate.network),
                        confidenceLevel = it.confidenceLevel,
                        classificationStatus = it.classificationStatus,
                        justification = it.justification,
                        mentions = it.candidate.mentionIds.mapNotNull(mentions::get),
                        scoringRules = it.appliedRules.map { rule -> ScoringRuleContributionResponse(rule.rule, rule.contribution, rule.premiseIds) },
                        addressActivity = null,
                        premises = it.premises.mapNotNull { premise -> toResponse(analysisId, premise, foundMentions[premise.mentionId]) },
                    )
                }
        val excluded =
            result?.excluded.orEmpty().map {
                ExcludedAddressResponse(
                    address = it.candidate.address,
                    network = Network.valueOf(it.candidate.network),
                    reason = it.reason,
                    classificationSource = it.classificationSource,
                    mentions = it.candidate.mentionIds.mapNotNull(mentions::get),
                )
            }
        logger.debug("Report read analysis={} attributions={} excluded={}", analysisId, attributions.size, excluded.size)
        return ReportResponse(history.toResponse(record), attributions, excluded, result?.limitations.orEmpty())
    }

    // a running Analysis has no result yet; an empty list would read as "nothing found" (E2.3)
    private fun finished(analysisId: String): AnalysisRecord {
        val record =
            repository.findById(analysisId).orElseThrow {
                logger.warn("Analysis not found id={}", analysisId)
                NotFoundException("Nie znaleziono analizy o podanym identyfikatorze.")
            }
        if (record.status == AnalysisStatus.RUNNING) {
            logger.warn("Analysis result requested while running id={}", analysisId)
            throw ConflictException("Analiza jeszcze trwa. Wynik będzie dostępny po jej zakończeniu.")
        }
        return record
    }

    private fun toResponse(mention: FoundMention) = MentionResponse(
        id = mention.id,
        sourceName = mention.sourceName,
        url = mention.url,
        collectedAt = mention.collectedAt,
        context = mention.context,
        detectedAddresses = mention.detectedAddresses.map(::detected),
        author = mention.author,
    )

    // the Premise and its Mention come from one snapshot; a missing Mention is a broken record, so the Premise is left out
    private fun toResponse(
        analysisId: String,
        premise: Premise,
        mention: FoundMention?,
    ): PremiseResponse? {
        if (mention == null) {
            logger.warn("Premise without its Mention analysis={} premise={}", analysisId, premise.id)
            return null
        }
        val facts = premise.facts
        return PremiseResponse(
            id = premise.id,
            mentionId = premise.mentionId,
            sourceName = mention.sourceName,
            url = mention.url,
            author = mention.author,
            collectedAt = mention.collectedAt,
            text = premise.window.text,
            addressStart = premise.window.addressStart,
            addressLength = premise.window.addressLength,
            status = premise.status,
            kind = premise.kind,
            points = premise.points,
            dismissalReason = premise.dismissalReason,
            assessedBy = premise.assessedBy,
            facts =
            PremiseFactsResponse(
                authorIsPerson = facts.authorIsPerson,
                underProfile = facts.underProfile,
                identifierTypes = facts.identifierTypes,
                identifierDistance = facts.identifierDistance,
                conflictingEmail = facts.conflictingEmail,
                addressCount = facts.addressCount,
                host = facts.host,
            ),
            answers = premise.answers?.let(::toResponse),
            undetermined = premise.undetermined,
            withdrawn = premise.withdrawn,
        )
    }

    private fun toResponse(answers: PremiseAnswers) = PremiseAnswersResponse(
        kind =
        with(answers.kind) {
            KindProbabilitiesResponse(ownDeclaration, saleOrSettlement, discussion, massComment)
        },
        declaration = toResponse(answers.declaration),
        warning = toResponse(answers.warning),
        requestId = answers.requestId,
    )

    private fun toResponse(probabilities: YesNoProbabilities) = YesNoProbabilitiesResponse(probabilities.yes, probabilities.no)

    private fun detected(rawAddress: String) = when (val validation = addressValidator.validate(rawAddress)) {
        is AddressValidation.Valid -> DetectedAddressResponse(rawAddress, validation.canonicalAddress.network, true, null)
        is AddressValidation.Invalid -> DetectedAddressResponse(rawAddress, null, false, validation.failure)
    }

    companion object {
        private val logger = logger()
    }
}
