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
import squad.nononotaa.backend.service.CaseService
import squad.nononotaa.backend.service.ScheduleService
import squad.nononotaa.backend.util.ApiController
import squad.nononotaa.backend.util.logger

@ApiController("/cases")
class CaseController(
    private val analysisHistory: AnalysisHistoryService,
    private val analysisComparison: AnalysisComparisonService,
    private val schedules: ScheduleService,
    private val cases: CaseService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCase(
        @Valid @RequestBody request: CreateCaseRequest,
    ): CaseResponse {
        logger.debug("POST /cases")
        return cases.create(request)
    }

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
    ): ScheduleResponse {
        logger.debug("GET /cases/{}/schedule", caseId)
        return schedules.get(caseId)
    }

    @PutMapping("/{caseId}/schedule")
    fun updateSchedule(
        @PathVariable caseId: String,
        @Valid @RequestBody request: UpdateScheduleRequest,
    ): ScheduleResponse {
        logger.debug("PUT /cases/{}/schedule enabled={}", caseId, request.enabled)
        return schedules.update(caseId, request)
    }

    companion object {
        private val logger = logger()
    }
}
