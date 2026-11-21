package squad.nononotaa.backend.service

import org.springframework.stereotype.Service
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.dto.AttributionResponse
import squad.nononotaa.backend.dto.DetectedAddressResponse
import squad.nononotaa.backend.dto.ExcludedAddressResponse
import squad.nononotaa.backend.dto.MentionResponse
import squad.nononotaa.backend.dto.ReportResponse
import squad.nononotaa.backend.dto.ScoringRuleContributionResponse
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
        val mentions = result?.mentions.orEmpty().associate { it.id to toResponse(it) }
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
                        scoringRules = it.appliedRules.map { rule -> ScoringRuleContributionResponse(rule.rule, rule.contribution) },
                        addressActivity = null,
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

    private fun detected(rawAddress: String) = when (val validation = addressValidator.validate(rawAddress)) {
        is AddressValidation.Valid -> DetectedAddressResponse(rawAddress, validation.canonicalAddress.network, true, null)
        is AddressValidation.Invalid -> DetectedAddressResponse(rawAddress, null, false, validation.failure)
    }

    companion object {
        private val logger = logger()
    }
}
