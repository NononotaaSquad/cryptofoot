package squad.nononotaa.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock
import java.time.Duration

@ConfigurationProperties("app.scheduler")
data class SchedulerProperties(
    val demoEnabled: Boolean = false,
    val grace: Duration = Duration.ofMinutes(2),
)

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SchedulerProperties::class)
class SchedulerConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
