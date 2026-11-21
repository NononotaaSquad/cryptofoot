package squad.nononotaa.backend.config

import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import squad.nononotaa.backend.analysis.AddressActivityCollector
import squad.nononotaa.backend.analysis.AnalysisOrchestrator
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.CandidatePreparation
import squad.nononotaa.backend.analysis.ConfidenceScoring
import squad.nononotaa.backend.analysis.ExclusionClassifier
import squad.nononotaa.backend.analysis.MentionSearch
import squad.nononotaa.backend.repository.AnalysisRepository
import squad.nononotaa.backend.service.AnalysisHistoryService
import squad.nononotaa.backend.service.AnalysisRerunService
import squad.nononotaa.backend.service.CaseIdentifierSource
import squad.nononotaa.backend.util.logger
import java.util.concurrent.Executors

@Configuration
class AnalysisConfig {
    // kept out of the context: an Executor bean would replace Spring Boot's applicationTaskExecutor
    private val analysisExecutor = Executors.newVirtualThreadPerTaskExecutor()

    @Bean
    fun analysisOrchestrator(
        search: MentionSearch,
        preparation: CandidatePreparation,
        exclusions: ExclusionClassifier,
        scoring: ConfidenceScoring,
        activity: AddressActivityCollector,
        recorder: AnalysisRecorder,
    ): AnalysisOrchestrator {
        logger.info(
            "Analysis flow wired: search={}, preparation={}, exclusions={}, scoring={}, activity={}",
            search.javaClass.simpleName,
            preparation.javaClass.simpleName,
            exclusions.javaClass.simpleName,
            scoring.javaClass.simpleName,
            activity.javaClass.simpleName,
        )
        return AnalysisOrchestrator(search, preparation, exclusions, scoring, activity, recorder)
    }

    @Bean
    fun analysisRerunService(
        orchestrator: AnalysisOrchestrator,
        repository: AnalysisRepository,
        identifiers: CaseIdentifierSource,
        history: AnalysisHistoryService,
    ) = AnalysisRerunService(orchestrator, repository, identifiers, history, analysisExecutor)

    @PreDestroy
    fun shutdown() {
        logger.info("Stopping Analysis executor")
        analysisExecutor.shutdownNow()
    }

    companion object {
        private val logger = logger()
    }
}
