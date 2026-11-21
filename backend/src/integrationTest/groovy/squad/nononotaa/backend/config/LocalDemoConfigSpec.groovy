package squad.nononotaa.backend.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import squad.nononotaa.backend.BaseSpec

@ActiveProfiles('local')
class LocalDemoConfigSpec extends BaseSpec {

    @Autowired
    SchedulerProperties properties

    @Autowired
    ApplicationContext context

    @Value('${app.scheduler.tick}')
    String tick

    def "the local profile shortens the scheduler check interval, enables the demo cadence and announces it"() {
        expect:
        tick == 'PT5S'
        properties.demoEnabled
        context.getBeansOfType(DemoModeNotice).size() == 1
    }
}