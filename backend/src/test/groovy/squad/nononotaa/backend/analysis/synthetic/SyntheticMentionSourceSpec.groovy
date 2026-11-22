package squad.nononotaa.backend.analysis.synthetic

import spock.lang.Specification
import squad.nononotaa.backend.analysis.AddressCandidateDetector
import squad.nononotaa.backend.model.Network
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class SyntheticMentionSourceSpec extends Specification {

    def detector = new AddressCandidateDetector()
    def mapper = JsonMapper.builder().addModule(new KotlinModule.Builder().build()).build()
    def source = new SyntheticMentionSource(mapper)

    static final BTC = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
    static final ETH = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'
    static final WEAK_BECH32 = 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'
    static final EXCHANGE = '0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae'
    static final MALFORMED = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfN0'

    static final FORUM_URL = 'https://forum.example/t/porady-p2p-od-zenka/123'
    static final X_URL = 'https://x.example/zenek_fx/status/555'
    static final P2P_URL = 'https://p2p.example/ad/987'

    def "fixture demo-v1 is versioned, marked synthetic and describes the fictional person"() {
        expect:
        source.fixture.version == 'demo-v1'
        source.fixture.synthetic
        source.fixture.person.firstName == 'Zenon'
        source.fixture.person.lastName == 'Fikcyjny'
        source.fixture.person.identifiers*.value.containsAll([
            'zenek_fx',
            '@zenek_fx',
            'zenon.fikcyjny@example.com',
            'zenek.fx@signal',
            'https://x.example/zenek_fx'
        ])
        source.sourceName == 'synthetic'
        source.synthetic
    }

    def "the first Analysis of a Case gets every fixture mention except the even-only ones as raw mentions"() {
        expect:
        source.findMentions([]).take(3)*.url == [FORUM_URL, X_URL, P2P_URL]
        source.findMentions([]).size() == 21
        source.findMentions([]).every {
            !it.content.isEmpty() && it.collectedAt != null
        }
    }

    def "fixture content yields the expected address candidates per mention"() {
        given:
        def byUrl = source.findMentions([]).collectEntries {
            [(it.url): it.content]
        }

        expect:
        detector.detect('m', byUrl[FORUM_URL])*.rawAddress == [BTC, ETH]
        detector.detect('m', byUrl[X_URL])*.rawAddress == [BTC, ETH]
        detector.detect('m', byUrl[X_URL])*.networkHint == [Network.BTC, Network.ETH]
        detector.detect('m', byUrl[P2P_URL])*.rawAddress == [WEAK_BECH32, EXCHANGE]
        detector.detect('m', byUrl[P2P_URL])*.networkHint == [Network.BTC, Network.ETH]
    }

    def "malformed address string in the fixture is not detected"() {
        given:
        def p2p = source.findMentions([]).find { it.url == P2P_URL }.content

        expect:
        p2p.contains(MALFORMED)
        detector.detect('m', p2p).every { it.rawAddress != MALFORMED }
    }

    def "scenario S11 flips between two states of the web: odd Analyses of a Case see one, even Analyses the other"() {
        given:
        def urls = { int earlier ->
            source.findMentions([], earlier)*.url.findAll {
                it.contains('oliwer_zmienny') || it.contains('konsola-retro') || it.contains('portfele-sprzetowe')
            }
        }

        expect:
        urls(0) == [
            'https://forum.example/t/portfele-sprzetowe/412',
            'https://market.example/oferta/konsola-retro-118'
        ]
        urls(1) == [
            'https://forum.example/t/portfele-sprzetowe/412',
            'https://social.example/oliwer_zmienny',
            'https://social.example/oliwer_zmienny/posty/2'
        ]
        urls(2) == urls(0)
        urls(3) == urls(1)
        // the single-argument form is the first Analysis
        source.findMentions([])*.url == source.findMentions([], 0)*.url
    }

    def "every other fixture mention is seen by every Analysis"() {
        expect:
        source.fixture.mentions.count { it.visibleIn != null } == 3
        source.findMentions([], 0).size() == 21
        source.findMentions([], 1).size() == 22
    }

    def "failing synthetic source simulates a source failure"() {
        when:
        new SyntheticFailingMentionSource().findMentions([])

        then:
        thrown(IllegalStateException)
    }
}
