package squad.nononotaa.backend.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import squad.nononotaa.backend.dto.AnalysisResponse
import squad.nononotaa.backend.dto.MentionResponse
import squad.nononotaa.backend.dto.ReportResponse
import squad.nononotaa.backend.service.AnalysisHistoryService
import squad.nononotaa.backend.util.ApiController
import squad.nononotaa.backend.util.logger

@ApiController("/analyses")
class AnalysisController(
    private val analysisHistory: AnalysisHistoryService,
) {
    @GetMapping("/{analysisId}")
    fun getAnalysis(
        @PathVariable analysisId: String,
    ): AnalysisResponse {
        logger.debug("GET /analyses/{}", analysisId)
        return analysisHistory.get(analysisId)
    }

    @GetMapping("/{analysisId}/mentions")
    fun listMentions(
        @PathVariable analysisId: String,
    ): List<MentionResponse> = TODO("E2.2/E2.3: Mentions with source, URL, collection time and detected addresses; empty list when none found")

    @GetMapping("/{analysisId}/report")
    fun getReport(
        @PathVariable analysisId: String,
    ): ReportResponse = TODO("E3.1/E5.1-E5.4: saved Attributions by Confidence Level desc, Excluded Addresses, limitations; never reruns the Analysis")

    companion object {
        private val logger = logger()
    }
}
