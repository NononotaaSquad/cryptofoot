package squad.nononotaa.backend.analysis.synthetic

import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import spock.lang.Specification
import squad.nononotaa.backend.analysis.MentionSource
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.ExtensionsKt

// the real application.yaml switches of the synthetic sources (ADR 0003); no Docker, no internet
class SyntheticSourcesSwitchSpec extends Specification {

    private static ApplicationContextRunner runner(String... properties) {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(SyntheticMentionSource, SyntheticFailingMentionSource)
                .withBean(ObjectMapper, { ExtensionsKt.jacksonObjectMapper() })
                .withPropertyValues(properties)
    }

    private static List<String> sourceNames(context) {
        context.getBeansOfType(MentionSource).values()*.sourceName.sort()
    }

    def "#situation: #expected"() {
        expect:
        runner(*properties).run { context ->
            assert context.startupFailure == null
            assert sourceNames(context) == expected
        }

        where:
        situation                                                    | properties || expected
        'local demo and tests (defaults)'                            | [] || [
            'synthetic',
            'synthetic-broken'
        ]
        'production deploy: demo data without the broken source'     | [
            'spring.profiles.active=prod',
            'sources.synthetic.enabled=true',
            'SOURCES_SYNTHETIC_BROKEN_ENABLED=false'
        ] || ['synthetic']
        'synthetic data switched off keeps the broken source off too' | [
            'sources.synthetic.enabled=false',
            'SOURCES_SYNTHETIC_BROKEN_ENABLED=true'
        ] || []
    }
}
