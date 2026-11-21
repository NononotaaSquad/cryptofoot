package squad.nononotaa.backend.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.MentionSource
import squad.nononotaa.backend.dto.TestRequest
import squad.nononotaa.backend.dto.TestResponse
import squad.nononotaa.backend.util.ApiController
import squad.nononotaa.backend.util.logger

@ApiController("/test")
class TestController(
    private val mentionSources: List<MentionSource>,
) {
    @GetMapping
    fun hello(): TestResponse {
        logger.debug("GET /test")
        return TestResponse("Hello")
    }

    @PostMapping
    fun greet(
        @Valid @RequestBody request: TestRequest,
    ): TestResponse {
        logger.debug("POST /test name={}", request.name)
        return TestResponse("Hello, ${request.name}")
    }

    @GetMapping("/scrape")
    fun scrape(
        @RequestParam identifier: String,
    ): Map<String, Any> {
        val identifiers = listOf(CaseIdentifier(IdentifierType.NICKNAME, identifier))
        val results = mentionSources.associate { source ->
            source.sourceName to try {
                val mentions = source.findMentions(identifiers)
                mapOf(
                    "count" to mentions.size,
                    "mentions" to mentions.map { mapOf("url" to it.url, "contentPreview" to it.content.take(200), "collectedAt" to it.collectedAt.toString()) },
                )
            } catch (e: Exception) {
                mapOf("error" to (e.message ?: e.javaClass.simpleName))
            }
        }
        return mapOf("identifier" to identifier, "sources" to results)
    }

    companion object {
        private val logger = logger()
    }
}
