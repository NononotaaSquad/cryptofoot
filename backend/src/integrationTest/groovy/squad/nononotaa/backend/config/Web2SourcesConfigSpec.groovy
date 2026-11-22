package squad.nononotaa.backend.config

import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import spock.lang.Shared
import spock.lang.Specification
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.MentionSource
import squad.nononotaa.testsupport.StubWeb2Server
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

/** The real application.yaml / application-prod.yaml / application-local.yaml wiring of the GitHub token; no Docker, no internet. */
class Web2SourcesConfigSpec extends Specification {

    static final String SECRET = 'ghp_test_secret_value'

    @Shared
    StubWeb2Server server = new StubWeb2Server()

    def setup() {
        server.reset()
        server.answer('/users/octo-dev', '{"login":"octo-dev","bio":"Donate 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"}')
        server.answer('/repos/octo-dev/octo-dev/readme', 'x', 404)
    }

    def cleanupSpec() {
        server.stop()
    }

    private ApplicationContextRunner runner(String profile, String... properties) {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(Web2SourcesConfig)
                .withBean(ObjectMapper, { JsonMapper.builder().build() })
                .withPropertyValues([
                    'spring.profiles.active=' + profile,
                    'sources.bitcointalk.enabled=false',
                    "sources.github.api-url=${server.url()}".toString()
                ] + properties.toList() as String[])
    }

    private static List<CaseIdentifier> login() {
        [
            new CaseIdentifier(IdentifierType.LOGIN, 'octo-dev')
        ]
    }

    def "production with the GitHub source enabled and no token fails at startup with a clear message that never holds a value"() {
        expect:
        runner('prod', *extra).run { context ->
            assert context.startupFailure != null
            def message = generateMessages(context.startupFailure)
            assert message.contains('SOURCES_GITHUB_TOKEN')
            assert !message.contains(SECRET)
        }

        where:
        extra << [
            [],
            ['sources.github.token='],
            ['sources.github.token=   ']
        ]
    }

    def "production with the GitHub source disabled needs no token"() {
        expect:
        runner('prod', 'sources.github.enabled=false').run { context ->
            assert context.startupFailure == null
            assert context.getBeansOfType(MentionSource).isEmpty()
        }
    }

    def "production sends the secret as a bearer header, trimmed, and nothing anonymous"() {
        expect:
        runner('prod', "sources.github.token=${SECRET}\n".toString()).run { context ->
            assert context.startupFailure == null
            def source = context.getBean('githubMentionSource', MentionSource)
            source.findMentions(login())
            assert server.requestLines() == [
                'GET /users/octo-dev',
                'GET /repos/octo-dev/octo-dev/readme'
            ]
            assert server.requests.every {
                it.headers['authorization'] == "Bearer ${SECRET}".toString()
            }
        }
    }

    def "an enabled source without a token is anonymous outside production, with no Authorization header"() {
        expect:
        runner('local', 'sources.github.enabled=true').run { context ->
            assert context.startupFailure == null
            context.getBean('githubMentionSource', MentionSource).findMentions(login())
            assert server.requests.size() == 2
            assert server.requests.every { it.headers['authorization'] == null }
        }
    }

    def "the local profile keeps the external sources off and needs no secret"() {
        expect:
        runner('local').run { context ->
            assert context.startupFailure == null
            assert context.getBeansOfType(MentionSource).isEmpty()
        }
    }

    def "the token never appears in the properties' text form"() {
        expect:
        !new Web2Properties.GitHub(true, URI.create('http://x'), SECRET, true, java.time.Duration.ofSeconds(1), 1, 1).toString().contains(SECRET)
    }

    private static String generateMessages(Throwable failure) {
        def messages = []
        for (def t = failure; t != null; t = t.cause) {
            messages << String.valueOf(t.message)
        }
        messages.join(' | ')
    }
}
