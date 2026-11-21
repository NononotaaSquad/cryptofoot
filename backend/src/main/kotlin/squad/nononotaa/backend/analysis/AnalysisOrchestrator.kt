package squad.nononotaa.backend.analysis

import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.util.logger

class AnalysisOrchestrator(
    private val search: MentionSearch,
    private val preparation: CandidatePreparation,
    private val exclusions: ExclusionClassifier,
    private val scoring: ConfidenceScoring,
    private val recorder: AnalysisRecorder,
) {
    fun start(request: AnalysisRequest): String {
        val analysisId = recorder.begin(request)
        logger.info("Analysis started id={} case={} trigger={}", analysisId, request.caseId, request.trigger)
        return analysisId
    }

    fun execute(
        analysisId: String,
        request: AnalysisRequest,
    ): AnalysisRun {
        val result = withLimitations(collect(request))
        recorder.finish(analysisId, result)
        logger.info("Analysis finished id={} status={} failures={}", analysisId, result.status, result.failures.size)
        return AnalysisRun(analysisId, result.status)
    }

    fun run(request: AnalysisRequest): AnalysisRun = execute(start(request), request)

    private fun collect(request: AnalysisRequest): AnalysisResult {
        val found =
            guarded(AnalysisStage.SEARCH) { search.search(request.identifiers) }
                ?: return failed(AnalysisStage.SEARCH, "MODULE_ERROR")
        val unavailable = found.sources.filter { it.status == SourceStatus.UNAVAILABLE }
        if (found.sources.isEmpty() || unavailable.size == found.sources.size) {
            val detail = if (found.sources.isEmpty()) "NO_SOURCE_CHECKED" else "ALL_SOURCES_UNAVAILABLE"
            return failed(AnalysisStage.SEARCH, detail, found)
        }
        val failures = unavailable.map { StageFailure(AnalysisStage.SEARCH, "SOURCE_UNAVAILABLE:${it.sourceName}") }
        if (found.mentions.isEmpty()) return completedOrPartial(failures, found)

        val prepared =
            guarded(AnalysisStage.CANDIDATE_PREPARATION) { preparation.prepare(found.mentions) }
                ?: return failed(AnalysisStage.CANDIDATE_PREPARATION, "MODULE_ERROR", found, failures)
        if (prepared.valid.isEmpty()) return completedOrPartial(failures, found, prepared)

        val classified =
            guarded(AnalysisStage.EXCLUSION) { exclusions.classify(prepared.valid) }
                ?: return failed(AnalysisStage.EXCLUSION, "MODULE_ERROR", found, failures, prepared)
        val allFailures =
            failures + classified.unclassified.map { StageFailure(AnalysisStage.EXCLUSION, "CLASSIFICATION_UNAVAILABLE:${it.address}") }
        val scorable = classified.accepted + classified.unclassified
        if (scorable.isEmpty()) return completedOrPartial(allFailures, found, prepared, classified)

        val scored =
            guarded(AnalysisStage.SCORING) { scoring.score(scorable, found.mentions) }
                ?: return failed(AnalysisStage.SCORING, "MODULE_ERROR", found, allFailures, prepared, classified)
        return completedOrPartial(allFailures, found, prepared, classified, flagUnclassified(scored, classified.unclassified))
    }

    private fun flagUnclassified(
        scored: ScoringResult,
        unclassified: List<Candidate>,
    ) = scored.copy(
        attributions =
        scored.attributions.map {
            if (it.candidate in unclassified) it.copy(classificationStatus = ClassificationStatus.UNAVAILABLE) else it
        },
    )

    private fun completedOrPartial(
        failures: List<StageFailure>,
        found: SearchResult,
        prepared: PreparedCandidates? = null,
        classified: ExclusionResult? = null,
        scored: ScoringResult? = null,
    ) = AnalysisResult(
        status = if (failures.isEmpty()) AnalysisStatus.COMPLETED else AnalysisStatus.PARTIAL,
        failures = failures,
        sources = found.sources,
        mentions = found.mentions,
        rejected = prepared?.rejected.orEmpty(),
        excluded = classified?.excluded.orEmpty(),
        unclassified = classified?.unclassified.orEmpty(),
        attributions = scored?.attributions.orEmpty(),
        algorithmVersion = scored?.algorithmVersion,
    )

    private fun failed(
        stage: AnalysisStage,
        detail: String,
        found: SearchResult? = null,
        earlier: List<StageFailure> = emptyList(),
        prepared: PreparedCandidates? = null,
        classified: ExclusionResult? = null,
    ) = completedOrPartial(earlier + StageFailure(stage, detail), found ?: SearchResult(emptyList(), emptyList()), prepared, classified)
        .copy(status = AnalysisStatus.FAILED)

    private fun withLimitations(result: AnalysisResult) = result.copy(
        limitations =
        listOfNotNull(
            ReportLimitation.SOURCE_UNAVAILABLE.takeIf { result.sources.any { it.status == SourceStatus.UNAVAILABLE } },
            ReportLimitation.CLASSIFICATION_UNAVAILABLE.takeIf { result.unclassified.isNotEmpty() },
            ReportLimitation.ANALYSIS_INCOMPLETE.takeIf { result.status != AnalysisStatus.COMPLETED },
        ),
    )

    private fun <T : Any> guarded(
        stage: AnalysisStage,
        block: () -> T,
    ): T? = try {
        block()
    } catch (e: Exception) {
        logger.error("Analysis stage failed stage={}", stage, e)
        null
    }

    companion object {
        private val logger = logger()
    }
}
