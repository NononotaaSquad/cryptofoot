package squad.nononotaa.backend.config

import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import spock.lang.Specification
import squad.nononotaa.backend.analysis.AddressActivityCollector
import squad.nononotaa.backend.service.ExclusionLabelDirectory
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

// the real application.yaml / application-prod.yaml / application-local.yaml switch of the live explorers (D23-012); no Docker, no internet
class AddressActivityProfilesSpec extends Specification {

    private static ApplicationContextRunner runner(String profile, String... properties) {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(AddressActivityConfig)
                .withBean(ObjectMapper, { JsonMapper.builder().build() })
                .withBean(ExclusionLabelDirectory, {
                    new ExclusionLabelDirectory([:])
                })
                // the production deploy runs without synthetic data (SOURCES_SYNTHETIC_ENABLED=false)
                .withPropertyValues(([
                    'spring.profiles.active=' + profile,
                    'sources.synthetic.enabled=false'
                ] + properties.toList()) as String[])
    }

    def "production reads Address Activity from the live explorers by default"() {
        expect:
        runner('prod').run { context ->
            assert context.startupFailure == null
            def properties = context.getBean(AddressActivityProperties)
            assert properties.liveEnabled
            assert properties.mempoolUrl == URI.create('https://mempool.space')
            assert properties.blockscoutUrl == URI.create('https://eth.blockscout.com')
            assert context.getBean(AddressActivityCollector) != null
        }
    }

    def "production can switch the live explorers off without a code change"() {
        expect:
        runner('prod', 'ADDRESS_ACTIVITY_LIVE_ENABLED=false').run { context ->
            assert !context.getBean(AddressActivityProperties).liveEnabled
        }
    }

    def "outside production the live explorers stay off: #profile"() {
        expect:
        runner(profile).run { context ->
            assert !context.getBean(AddressActivityProperties).liveEnabled
        }

        where:
        profile << ['local', 'default']
    }
}
