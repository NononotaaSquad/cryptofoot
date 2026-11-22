package squad.nononotaa.backend.config

import org.springframework.ai.systemone.SystemOneDecisionModel
import org.springframework.ai.systemone.api.SystemOneClient
import org.springframework.ai.systemone.api.SystemOneRetryUtils
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import squad.nononotaa.backend.analysis.PremiseAssessment
import squad.nononotaa.backend.service.ModelPremiseAssessment
import squad.nononotaa.backend.service.RulesOnlyPremiseAssessment
import squad.nononotaa.backend.util.logger
import java.net.URI
import java.net.http.HttpClient
import java.time.Duration

/**
 * The decision model TypeSafe "Jev" (D5, D13, P20). Off by default; on only with `ASSESSMENT_MODEL_ENABLED=true`
 * together with the key in `ASSESSMENT_MODEL_API_KEY`. [apiKey] never appears in [toString] or in a log.
 */
@ConfigurationProperties("app.assessment.model")
data class AssessmentModelProperties(
    val enabled: Boolean = false,
    val baseUrl: URI = URI.create("https://api.typesafe.ai"),
    // the docs name no pinned snapshot id; the version the server echoes for this alias is what an Analysis stores
    val model: String = "jev-latest",
    val timeout: Duration = Duration.ofSeconds(5),
    val apiKey: String = "",
) {
    override fun toString() = "AssessmentModelProperties(enabled=$enabled, baseUrl=$baseUrl, model=$model, timeout=$timeout, apiKey=${if (apiKey.isBlank()) "<none>" else "<set>"})"
}

/**
 * Exposes the one [PremiseAssessment]. The Spring AI client is built here by hand, not auto-configured
 * (`spring.ai.model.decision: none`), so the app starts and runs keyless.
 */
@Configuration
@EnableConfigurationProperties(AssessmentModelProperties::class)
class AssessmentConfig {
    @Bean
    fun premiseAssessment(properties: AssessmentModelProperties): PremiseAssessment {
        val assessment =
            when {
                !properties.enabled -> RulesOnlyPremiseAssessment()

                properties.apiKey.isBlank() -> {
                    logger.warn("Decision model is enabled but ASSESSMENT_MODEL_API_KEY is not set; Premises are assessed by rules only")
                    RulesOnlyPremiseAssessment()
                }

                else -> ModelPremiseAssessment(decisionModel(properties), properties.model, properties.timeout)
            }
        logger.info("Premise assessment: mode={}, host={}, model={}", assessment.mode, properties.baseUrl.host, properties.model)
        return assessment
    }

    private fun decisionModel(properties: AssessmentModelProperties): SystemOneDecisionModel {
        // own transport: connect and read timeout per request (the builder's timeout() only sets the read timeout)
        val http = HttpClient.newBuilder().connectTimeout(properties.timeout).build()
        val requestFactory = JdkClientHttpRequestFactory(http).apply { setReadTimeout(properties.timeout) }
        val client =
            SystemOneClient
                .builder()
                .apiKey(properties.apiKey)
                .baseUrl(properties.baseUrl.toString())
                .defaultModel(properties.model)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                // P18: no retry; the default template retries twice
                .retryTemplate(SystemOneRetryUtils.noRetryTemplate())
                .build()
        return SystemOneDecisionModel.builder(client).build()
    }

    companion object {
        private val logger = logger()
    }
}
