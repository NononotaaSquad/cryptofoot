package squad.nononotaa.backend.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import squad.nononotaa.backend.util.ApiController

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix(API_PATH_PREFIX, HandlerTypePredicate.forAnnotation(ApiController::class.java))
    }

    // Public API without cookies: any origin may call it (both Cloud Run frontend URLs, localhost, ...).
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    }

    companion object {
        const val API_PATH_PREFIX = "/api/{version}/"
    }
}
