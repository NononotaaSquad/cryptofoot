package squad.nononotaa.backend.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import squad.nononotaa.backend.dto.AnalysisResponse
import squad.nononotaa.backend.dto.MentionResponse
import squad.nononotaa.backend.dto.ReportResponse
import squad.nononotaa.backend.service.AnalysisHistoryService
import squad.nononotaa.backend.service.AnalysisReportService
import squad.nononotaa.backend.util.ApiController
import squad.nononotaa.backend.util.logger

@ApiController("/analyses")
class AnalysisController(
    private val analysisHistory: AnalysisHistoryService,
    private val reports: AnalysisReportService,
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
    ): List<MentionResponse> {
        logger.debug("GET /analyses/{}/mentions", analysisId)
        return reports.mentions(analysisId)
    }

    @GetMapping("/{analysisId}/report")
    fun getReport(
        @PathVariable analysisId: String,
    ): ReportResponse {
        logger.debug("GET /analyses/{}/report", analysisId)
        return reports.report(analysisId)
    }

    companion object {
        private val logger = logger()
    }
}
