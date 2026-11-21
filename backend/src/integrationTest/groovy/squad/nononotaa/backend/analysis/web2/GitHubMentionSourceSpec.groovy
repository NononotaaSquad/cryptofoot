package squad.nononotaa.backend.analysis.web2

import java.time.Clock
import java.time.Duration
import spock.lang.Shared
import spock.lang.Specification
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.SourceNotCheckedException
import squad.nononotaa.backend.analysis.SourceUnavailableException
import squad.nononotaa.testsupport.StubWeb2Server
import squad.nononotaa.testsupport.Web2Fixtures
import tools.jackson.databind.json.JsonMapper

class GitHubMentionSourceSpec extends Specification {

    @Shared
    StubWeb2Server server = new StubWeb2Server()

    def setup() {
        server.reset()
    }

    def cleanupSpec() {
        server.stop()
    }

    private GitHubMentionSource source(String token = '', Duration timeout = Duration.ofMillis(500)) {
        new GitHubMentionSource(Web2Fixtures.github(server, token, timeout), JsonMapper.builder().build(), Clock.systemUTC())
    }

    private static List<CaseIdentifier> login(String value = 'octo-dev') {
        [
            new CaseIdentifier(IdentifierType.LOGIN, value)
        ]
    }

    def "profile and profile README become two mentions with canonical URLs, the real text and the account as author"() {
        given:
        server.answer('/users/octo-dev', Web2Fixtures.githubUser())
        server.answer('/repos/Octo-Dev/Octo-Dev/readme', Web2Fixtures.longReadme())

        when:
        def mentions = source().findMentions(login())

        then:
        !source().synthetic
        source().sourceName == 'github'
        mentions*.url == [
            'https://github.com/Octo-Dev',
            'https://github.com/Octo-Dev/Octo-Dev'
        ]
        mentions*.author == ['Octo-Dev', 'Octo-Dev']
        mentions[0].content == "Octo Dev\nBuilder. Donate BTC to ${Web2Fixtures.BTC}\nWarsaw"
        mentions[1].content == Web2Fixtures.longReadme()
        mentions.every { it.collectedAt != null }
        !mentions.any {
            it.content.contains('octo-dev') && !it.content.contains('Octo Dev')
        }
        server.requestLines() == [
            'GET /users/octo-dev',
            'GET /repos/Octo-Dev/Octo-Dev/readme'
        ]
        server.requests[1].headers['accept'] == 'application/vnd.github.raw+json'
        server.requests[0].headers['authorization'] == null
    }

    def "the optional token is sent as a bearer header and nothing else is added"() {
        given:
        server.answer('/users/octo-dev', Web2Fixtures.githubUser())
        server.answer('/repos/Octo-Dev/Octo-Dev/readme', 'readme', 404)

        when:
        source('secret-token').findMentions(login())

        then:
        server.requests*.headers.every {
            it['authorization'] == 'Bearer secret-token'
        }
    }

    def "a rejected token is unavailability and the request is never repeated anonymously"() {
        given:
        server.answer('/users/octo-dev', '{"message":"Bad credentials"}', status)

        when:
        source('revoked-token').findMentions(login())

        then:
        thrown(SourceUnavailableException)
        server.requests.size() == 1
        server.requests[0].headers['authorization'] == 'Bearer revoked-token'

        where:
        status << [401, 403]
    }

    def "a missing profile README does not erase the valid profile"() {
        given:
        server.answer('/users/octo-dev', Web2Fixtures.githubUser())
        server.answer('/repos/Octo-Dev/Octo-Dev/readme', '{"message":"Not Found"}', 404)

        when:
        def mentions = source().findMentions(login())

        then:
        mentions*.url == ['https://github.com/Octo-Dev']
    }

    def "a profile URL gives the login, and a missing user is a valid zero without fetching the README"() {
        when:
        def mentions = source().findMentions([
            new CaseIdentifier(IdentifierType.PROFILE_URL, 'https://github.com/Ghost-User')
        ])

        then:
        mentions == []
        server.requestLines() == ['GET /users/Ghost-User']
    }

    def "a profile without any public text and without README is a valid zero"() {
        given:
        server.answer('/users/octo-dev', '{"login":"octo-dev","name":null,"bio":null,"blog":"","company":null,"location":null}')
        server.answer('/repos/octo-dev/octo-dev/readme', '', 404)

        expect:
        source().findMentions(login()) == []
    }

    def "an access error, a rate limit, a server error or unreadable JSON is unavailability, never zero results"() {
        given:
        server.answer('/users/octo-dev', body, status, headers)

        when:
        source().findMentions(login())

        then:
        thrown(SourceUnavailableException)

        where:
        status | headers                          | body
        401    | [:]                              | '{"message":"Bad credentials"}'
        403    | [:]                              | '{"message":"Forbidden"}'
        403    | ['X-RateLimit-Remaining': '0']   | '{"message":"API rate limit exceeded"}'
        429    | [:]                              | ''
        404    | ['X-RateLimit-Remaining': '0']   | '{"message":"Not Found"}'
        500    | [:]                              | 'oops'
        200    | [:]                              | '<html>not json</html>'
    }

    def "an access error on the README after a valid profile makes the whole source unavailable"() {
        given:
        server.answer('/users/octo-dev', Web2Fixtures.githubUser())
        server.answer('/repos/Octo-Dev/Octo-Dev/readme', '', 403)

        when:
        source().findMentions(login())

        then:
        thrown(SourceUnavailableException)
    }

    def "a timeout is unavailability"() {
        given:
        server.answer('/users/octo-dev', Web2Fixtures.githubUser(), 200, [:], 1500)

        when:
        source('', Duration.ofMillis(200)).findMentions(login())

        then:
        thrown(SourceUnavailableException)
    }

    def "unsupported input is not checked and nothing is requested"() {
        when:
        source().findMentions(identifiers)

        then:
        thrown(SourceNotCheckedException)
        server.requests.isEmpty()

        where:
        identifiers << [
            [
                new CaseIdentifier(IdentifierType.EMAIL, 'a@b.pl')
            ],
            [
                new CaseIdentifier(IdentifierType.NICKNAME, 'two words')
            ],
            [
                new CaseIdentifier(IdentifierType.NICKNAME, '../etc')
            ],
            [
                new CaseIdentifier(IdentifierType.PROFILE_URL, 'https://example.org/octo-dev')
            ],
            [
                new CaseIdentifier(IdentifierType.PROFILE_URL, 'https://github.com/orgs/some-org')
            ],
            [
                new CaseIdentifier(IdentifierType.FULL_NAME, 'Jan Kowalski')
            ],
            []
        ]
    }

    def "the same account given as login and as profile URL is fetched once"() {
        given:
        server.answer('/users/octo-dev', '', 404)

        when:
        source().findMentions([
            new CaseIdentifier(IdentifierType.LOGIN, 'octo-dev'),
            new CaseIdentifier(IdentifierType.NICKNAME, 'Octo-Dev'),
            new CaseIdentifier(IdentifierType.PROFILE_URL, 'https://github.com/octo-dev')
        ])

        then:
        server.requests.size() == 1
    }
}
