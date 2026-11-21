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

class BitcointalkMentionSourceSpec extends Specification {

    static final String PROFILE_URL = 'https://bitcointalk.org/index.php?action=profile;u=1234'
    static final String PROFILE_PATH = '/index.php?action=profile;u=1234'

    @Shared
    StubWeb2Server server = new StubWeb2Server()

    def setup() {
        server.reset()
    }

    def cleanupSpec() {
        server.stop()
    }

    private BitcointalkMentionSource source(Duration timeout = Duration.ofMillis(500), int maxPosts = 20) {
        new BitcointalkMentionSource(Web2Fixtures.bitcointalk(server, timeout, maxPosts), Clock.systemUTC())
    }

    private static List<CaseIdentifier> url(String value) {
        [
            new CaseIdentifier(IdentifierType.PROFILE_URL, value)
        ]
    }

    private static String thread() {
        Web2Fixtures.bitcointalkThread([
            Web2Fixtures.bitcointalkPost('101', 'alice', "Send to ${Web2Fixtures.BTC} thanks<br>bye"),
            Web2Fixtures.bitcointalkPost('102', 'bob',
            "<div class=\"quoteheader\">Quote from: alice</div><div class=\"quote\">alice wrote ${Web2Fixtures.ETH}</div>I disagree with alice"),
            Web2Fixtures.bitcointalkPost('103', 'carol', 'third post'),
        ])
    }

    def "the profile gives the Bitcoin address field and the signature under the profile's name and canonical URL"() {
        given:
        server.answer(PROFILE_PATH, Web2Fixtures.bitcointalkProfile())

        when:
        def mentions = source().findMentions(url(PROFILE_URL))

        then:
        !source().synthetic
        source().sourceName == 'bitcointalk'
        mentions.size() == 1
        mentions[0].url == PROFILE_URL
        mentions[0].author == 'Satoshi_Fan'
        mentions[0].content == "Bitcoin address: ${Web2Fixtures.BTC}\nSignature: Tip me: ${Web2Fixtures.BECH32}"
        server.requestLines() == ["GET ${PROFILE_PATH}"]
    }

    def "a profile without address and signature, or a page of a missing profile, is a valid zero"() {
        given:
        server.answer(PROFILE_PATH, page)

        expect:
        source().findMentions(url(PROFILE_URL)) == []

        where:
        page << [
            '<html><body><table><tr><td><b>Name: </b></td><td>Empty_One</td></tr></table></body></html>',
            '<html><body>An Error Has Occurred! The user whose profile you are trying to view does not exist.</body></html>'
        ]
    }

    def "an unrecognised page is unavailability, not zero"() {
        given:
        server.answer(PROFILE_PATH, '<html><body>Checking your browser before accessing</body></html>')

        when:
        source().findMentions(url(PROFILE_URL))

        then:
        thrown(SourceUnavailableException)
    }

    def "a thread gives every post under its own author without quoted text of others"() {
        given:
        server.answer('/index.php?topic=555.0', thread())

        when:
        def mentions = source().findMentions(url('https://bitcointalk.org/index.php?topic=555.0'))

        then:
        mentions*.author == ['alice', 'bob', 'carol']
        mentions*.url == [
            'https://bitcointalk.org/index.php?topic=555.msg101#msg101',
            'https://bitcointalk.org/index.php?topic=555.msg102#msg102',
            'https://bitcointalk.org/index.php?topic=555.msg103#msg103'
        ]
        mentions[0].content == "Send to ${Web2Fixtures.BTC} thanks\nbye"
        mentions[1].content == 'I disagree with alice'
        !mentions[1].content.contains(Web2Fixtures.ETH)
    }

    def "a post URL keeps only that post and the number of posts is limited"() {
        given:
        server.answer('/index.php?topic=555.msg102', thread())
        server.answer('/index.php?topic=555.0', thread())

        when:
        def one = source().findMentions(url('https://bitcointalk.org/index.php?topic=555.msg102#msg102'))
        def limited = source(Duration.ofMillis(500), 2).findMentions(url('https://bitcointalk.org/index.php?topic=555.0'))

        then:
        one*.author == ['bob']
        limited*.author == ['alice', 'bob']
    }

    def "a missing thread is a valid zero"() {
        given:
        server.answer('/index.php?topic=555.0', '<html><body>The topic or board you are looking for appears to be either missing or off limits to you.</body></html>')

        expect:
        source().findMentions(url('https://bitcointalk.org/index.php?topic=555.0')) == []
    }

    def "access errors and a timeout are unavailability"() {
        given:
        server.answer(PROFILE_PATH, 'x', status)

        when:
        source().findMentions(url(PROFILE_URL))

        then:
        thrown(SourceUnavailableException)

        where:
        status << [403, 429, 503]
    }

    def "a slow forum is unavailability"() {
        given:
        server.answer(PROFILE_PATH, Web2Fixtures.bitcointalkProfile(), 200, [:], 1500)

        when:
        source(Duration.ofMillis(200)).findMentions(url(PROFILE_URL))

        then:
        thrown(SourceUnavailableException)
    }

    def "a nickname, a nickname URL or a foreign URL is not mapped to a profile and nothing is requested"() {
        when:
        source().findMentions(identifiers)

        then:
        thrown(SourceNotCheckedException)
        server.requests.isEmpty()

        where:
        identifiers << [
            [
                new CaseIdentifier(IdentifierType.NICKNAME, 'Satoshi_Fan')
            ],
            [
                new CaseIdentifier(IdentifierType.LOGIN, '1234')
            ],
            url('https://bitcointalk.org/index.php?action=profile;user=Satoshi_Fan'),
            url('https://bitcointalk.org/index.php?board=1.0'),
            url('https://evil.example/index.php?action=profile;u=1234'),
            []
        ]
    }
}
