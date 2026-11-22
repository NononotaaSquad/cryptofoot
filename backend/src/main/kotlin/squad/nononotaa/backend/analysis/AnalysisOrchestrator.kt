package squad.nononotaa.backend.analysis

import squad.nononotaa.backend.model.AddressActivityStatus
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AssessmentMode
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.util.logger

class AnalysisOrchestrator(
    private val search: MentionSearch,
    private val preparation: CandidatePreparation,
    private val exclusions: ExclusionClassifier,
    private val assessment: PremiseAssessment,
    private val scoring: ConfidenceScoring,
    private val activity: AddressActivityCollector,
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
            guarded(AnalysisStage.SEARCH) { search.search(request.identifiers, request.earlierAnalyses) }
                ?: return failed(AnalysisStage.SEARCH, "MODULE_ERROR")
        val unavailable = found.sources.filter { it.status == SourceStatus.UNAVAILABLE }
        if (found.sources.none { it.status == SourceStatus.CHECKED }) {
            val detail = if (unavailable.isEmpty()) "NO_SOURCE_CHECKED" else "ALL_SOURCES_UNAVAILABLE"
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

        // after exclusion and before scoring, so scoring stays pure (P17); asked again on every Analysis, never reused (D7)
        val assessed = assess(scorable, found.mentions, request.identifiers)
        val scored =
            guarded(AnalysisStage.SCORING) {
                scoring.score(
                    ScoringInput(
                        classified.accepted,
                        classified.unclassified,
                        found.mentions,
                        request.identifiers,
                        assessed.answers,
                        assessed.mode,
                    ),
                )
            }
                ?: return failed(AnalysisStage.SCORING, "MODULE_ERROR", found, allFailures, prepared, classified)
        val (withActivity, activityFailures) = withAddressActivity(flagUnclassified(scored, classified.unclassified))
        return completedOrPartial(allFailures + activityFailures, found, prepared, classified, withActivity)
            .copy(
                assessmentMode = assessed.mode,
                modelVersion = assessed.modelVersion,
                questionSetVersion = QUESTION_SET_VERSION.takeIf { assessed.mode == AssessmentMode.WITH_MODEL },
                // kept by withLimitations; the result has no other place to carry "some Premise fell back"
                limitations = listOfNotNull(ReportLimitation.MODEL_ASSESSMENT_UNAVAILABLE.takeIf { assessed.fellBack }),
            )
    }

    // Address Activity is supplementary: its failure makes the Analysis partial, never failed (D23-005)
    private fun withAddressActivity(scored: ScoringResult): Pair<ScoringResult, List<StageFailure>> {
        val outcomes = guarded(AnalysisStage.ADDRESS_ACTIVITY) { activity.collect(scored.attributions) }
        val attributions =
            scored.attributions.map {
                it.copy(addressActivity = if (outcomes == null) AddressActivityOutcome(AddressActivityStatus.UNAVAILABLE) else outcomes[it.candidate])
            }
        val failures =
            if (outcomes == null) {
                listOf(StageFailure(AnalysisStage.ADDRESS_ACTIVITY, "MODULE_ERROR"))
            } else {
                attributions
                    .filter { it.addressActivity?.status == AddressActivityStatus.UNAVAILABLE }
                    .map { StageFailure(AnalysisStage.ADDRESS_ACTIVITY, "ADDRESS_ACTIVITY_UNAVAILABLE:${it.candidate.address}") }
            }
        return scored.copy(attributions = attributions) to failures
    }

    // only Premises of accepted and unclassified addresses are sent; Rejected and Excluded Addresses never (P18)
    private fun assess(
        scorable: List<Candidate>,
        mentions: List<FoundMention>,
        identifiers: List<CaseIdentifier>,
    ): Assessed {
        val mode = assessment.mode
        if (mode != AssessmentMode.WITH_MODEL) return Assessed(mode)
        val byId = mentions.associateBy { it.id }
        val premises =
            scorable
                .flatMap { candidate ->
                    premiseWindows(candidate, byId).map { (mention, window) ->
                        PremiseToAssess(Premise.idOf(mention.id, candidate.address), candidate.address, window.text, premiseFacts(mention, window, identifiers))
                    }
                }.distinctBy { it.premiseId }
        val assessments =
            try {
                assessment.assess(premises, identifiers)
            } catch (e: Exception) {
                // the class name only: a message could carry a text window or a body
                logger.warn("Premise assessment failed, every Premise falls back to rules premises={} error={}", premises.size, e.javaClass.simpleName)
                PremiseAssessments()
            }
        val sent = premises.map { it.premiseId }.toSet()
        val answers = assessments.answers.filterKeys { it in sent }
        logger.info("Premises assessed sent={} answered={} fellBack={}", sent.size, answers.size, sent.size - answers.size)
        return Assessed(mode, answers, assessments.modelVersion, fellBack = answers.size < sent.size)
    }

    private data class Assessed(
        val mode: AssessmentMode,
        val answers: Map<String, PremiseAnswers> = emptyMap(),
        val modelVersion: String? = null,
        val fellBack: Boolean = false,
    )

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
            ReportLimitation.MODEL_ASSESSMENT_UNAVAILABLE.takeIf { it in result.limitations },
            ReportLimitation.BLOCKCHAIN_DATA_UNAVAILABLE.takeIf {
                result.attributions.any { it.addressActivity?.status == AddressActivityStatus.UNAVAILABLE }
            },
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
