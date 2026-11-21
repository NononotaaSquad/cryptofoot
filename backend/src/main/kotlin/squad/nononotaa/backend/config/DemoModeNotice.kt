package squad.nononotaa.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import squad.nononotaa.backend.util.logger

@Component
@ConditionalOnProperty("app.scheduler.demo-enabled", havingValue = "true")
class DemoModeNotice(
    @Value("\${app.scheduler.tick:PT30S}") private val tick: String,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        logger.warn("DEMO mode active: the scheduler checks due terms every {} and the demo cadence is enabled. Not for production.", tick)
    }

    companion object {
        private val logger = logger()
    }
}
