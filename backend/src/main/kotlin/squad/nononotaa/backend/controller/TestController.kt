package squad.nononotaa.backend.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import squad.nononotaa.backend.dto.TestRequest
import squad.nononotaa.backend.dto.TestResponse
import squad.nononotaa.backend.util.ApiController
import squad.nononotaa.backend.util.logger

@ApiController("/test")
class TestController {
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

    companion object {
        private val logger = logger()
    }
}
