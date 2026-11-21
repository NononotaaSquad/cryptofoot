package squad.nononotaa.backend.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import squad.nononotaa.backend.dto.AnalysisComparisonResponse
import squad.nononotaa.backend.dto.AnalysisResponse
import squad.nononotaa.backend.dto.CaseResponse
import squad.nononotaa.backend.dto.CreateCaseRequest
import squad.nononotaa.backend.dto.ScheduleResponse
import squad.nononotaa.backend.dto.UpdateScheduleRequest
import squad.nononotaa.backend.service.AnalysisComparisonService
import squad.nononotaa.backend.service.AnalysisHistoryService
import squad.nononotaa.backend.util.ApiController
import squad.nononotaa.backend.util.logger

@ApiController("/cases")
class CaseController(
    private val analysisHistory: AnalysisHistoryService,
    private val analysisComparison: AnalysisComparisonService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCase(
        @Valid @RequestBody request: CreateCaseRequest,
    ): CaseResponse = TODO("E1.1-E1.3: reject without a narrowing identifier (400 listing the fields), persist the Case")

    @GetMapping
    fun listCases(): List<CaseResponse> = TODO("E6.1: all Cases, newest first, with their latest Analysis")

    @GetMapping("/{caseId}")
    fun getCase(
        @PathVariable caseId: String,
    ): CaseResponse = TODO("E6.1: Case with its latest Analysis; 404 when missing")

    @PostMapping("/{caseId}/analyses")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun startAnalysis(
        @PathVariable caseId: String,
    ): AnalysisResponse = TODO("E2.1/E6.2: new MANUAL Analysis in RUNNING state with the Case's current identifiers; 409 when one is already running")

    @GetMapping("/{caseId}/analyses")
    fun listAnalyses(
        @PathVariable caseId: String,
    ): List<AnalysisResponse> {
        logger.debug("GET /cases/{}/analyses", caseId)
        return analysisHistory.listByCase(caseId)
    }

    @GetMapping("/{caseId}/comparison")
    fun compareAnalyses(
        @PathVariable caseId: String,
        @RequestParam earlierAnalysisId: String,
        @RequestParam laterAnalysisId: String,
    ): AnalysisComparisonResponse {
        logger.debug("GET /cases/{}/comparison earlier={} later={}", caseId, earlierAnalysisId, laterAnalysisId)
        return analysisComparison.compare(caseId, earlierAnalysisId, laterAnalysisId)
    }

    @GetMapping("/{caseId}/schedule")
    fun getSchedule(
        @PathVariable caseId: String,
    ): ScheduleResponse = TODO("E6.4: schedule state, next execution and last attempt outcome")

    @PutMapping("/{caseId}/schedule")
    fun updateSchedule(
        @PathVariable caseId: String,
        @Valid @RequestBody request: UpdateScheduleRequest,
    ): ScheduleResponse = TODO("E6.4: enable/disable periodic Analyses; cadence and timezone pending E6-D3")

    companion object {
        private val logger = logger()
    }
}
