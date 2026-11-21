package squad.nononotaa.backend.service

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.ScoringInput
import squad.nononotaa.backend.model.ClassificationStatus

class ConfidenceScoringServiceSpec extends Specification {

    static final Instant AT = Instant.parse('2026-11-21T10:00:00Z')
    static final String BTC = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
    static final String ETH_EIP55 = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'
    static final String ETH = ETH_EIP55.toLowerCase()
    static final CaseIdentifier NICKNAME = new CaseIdentifier(IdentifierType.NICKNAME, 'zenek_fx')
    static final CaseIdentifier PROFILE = new CaseIdentifier(IdentifierType.PROFILE_URL, 'https://x.example/zenek_fx')
    static final CaseIdentifier EMAIL = new CaseIdentifier(IdentifierType.EMAIL, 'zenon.fikcyjny@example.com')
    static final CaseIdentifier FULL_NAME = new CaseIdentifier(IdentifierType.FULL_NAME, 'Zenon Fikcyjny')
    static final CaseIdentifier WALLET = new CaseIdentifier(IdentifierType.WALLET_ADDRESS, ETH)

    static final String NO_LINK = 'Żadna wzmianka nie łączy tego adresu z danymi badanej osoby.'
    static final String NICKNAME_ONLY = 'Adres łączy z badaną osobą tylko nick lub login, które mogą należeć do innej osoby.'

    def scoring = new ConfidenceScoringService()

    private static FoundMention mention(String id, String url, String context) {
        new FoundMention(id, 'synthetic', url, AT, context, [])
    }

    private ScoredAttribution scoreOne(List<FoundMention> mentions, List<CaseIdentifier> identifiers, String address = BTC) {
        def network = address.startsWith('0x') ? 'ETH' : 'BTC'
        scoring.score(new ScoringInput([
            new Candidate(network, address, mentions*.id)
        ], [], mentions, identifiers)).attributions[0]
    }

    def "an address on the person's own profile gets the own-profile base, its rule, a Justification sentence and the algorithm version"() {
        when:
        def result = scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, ['m1'])
        ], [], [
            mention('m1', 'https://x.example/zenek_fx/status/555', "@zenek_fx: trust wallet — BTC ${BTC}. DM open.")
        ], [NICKNAME, PROFILE]))

        then:
        result.algorithmVersion == 'confidence-1'
        result.attributions.size() == 1
        with(result.attributions[0]) {
            confidenceLevel == 80
            appliedRules == [
                new AppliedRule('OWN_PROFILE', 80)
            ]
            justification == 'Adres został opublikowany na profilu badanej osoby (x.example).'
            classificationStatus == ClassificationStatus.NOT_EXCLUDED
        }
    }

    def "a single Mention linked by the nickname gets the base of its source kind: #expectedRule"() {
        when:
        def attribution = scoreOne([mention('m1', url, context)], [NICKNAME])

        then:
        attribution.confidenceLevel == level
        attribution.appliedRules*.rule == [expectedRule, 'NICKNAME_ONLY']
        attribution.justification.endsWith(NICKNAME_ONLY)

        where:
        url                             | context || expectedRule          | level
        'https://p2p.example/ad/1'      | "Ogłoszenie od zenek_fx. Adres: ${BTC}." || 'SALE_OR_SETTLEMENT'  | 60
        'https://forum.example/t/1'     | "zenek_fx: rozliczenie za konsultację na ${BTC}." || 'SALE_OR_SETTLEMENT'  | 60
        'https://forum.example/t/2'     | "zenek_fx: OGLOSZENIE bez polskich znakow ${BTC}" || 'SALE_OR_SETTLEMENT'  | 60
        'https://forum.example/t/3'     | "zenek_fx pisze: mój adres to ${BTC}." || 'DISCUSSION'          | 40
        'https://social.example/p/9'    | "GIVEAWAY! zenek_fx: ${BTC}" || 'MASS_COMMENT'        | 10
        'https://shop.example/t/4'      | "zenek_fx sprzedaje, promocja dla pierwszych: ${BTC}" || 'MASS_COMMENT'        | 10
    }

    def "the source kind sentence names the service and the Justification has no numbers"() {
        expect:
        scoreOne([
            mention('m1', 'https://forum.example/t/3', "zenek_fx pisze: mój adres to ${BTC}.")
        ], [NICKNAME]).justification ==
        'Adres pojawił się w dyskusji (forum.example); taki ślad wymaga potwierdzenia drugim źródłem. ' + NICKNAME_ONLY
    }

    def "a page named after the nickname counts as the own profile only with a second identifier of the person: #description"() {
        when:
        def attribution = scoreOne([
            mention('m1', 'https://github.com/zenek_fx', context)
        ], identifiers)

        then:
        attribution.appliedRules*.rule == rules
        attribution.confidenceLevel == level

        where:
        description           | identifiers           | context || rules                           | level
        'e-mail'              | [NICKNAME, EMAIL]     | "zenek_fx, kontakt zenon.fikcyjny@example.com, BTC ${BTC}" || ['OWN_PROFILE']                 | 80
        'full name'           | [NICKNAME, FULL_NAME] | "Zenon Fikcyjny (zenek_fx), BTC ${BTC}" || ['OWN_PROFILE']                 | 80
        'nickname alone'      | [NICKNAME]            | "zenek_fx, BTC ${BTC}" || ['DISCUSSION', 'NICKNAME_ONLY'] | 40
    }

    def "Mentions on services of different origin with their own sentence add the independent-source bonus once"() {
        when:
        def attribution = scoreOne([
            mention('m1', 'https://x.example/zenek_fx/status/555', "@zenek_fx: trust wallet — BTC ${BTC}."),
            mention('m2', 'https://p2p.example/ad/987', "Ogłoszenie P2P od zenek_fx. Płatność na ${BTC}."),
            mention('m3', 'https://www.forum.example/t/1', "zenek_fx pisze: mój adres to ${BTC}."),
        ], [NICKNAME, PROFILE])

        then:
        attribution.confidenceLevel == 95
        attribution.appliedRules == [
            new AppliedRule('OWN_PROFILE', 80),
            new AppliedRule('INDEPENDENT_SOURCES', 15)
        ]
        attribution.justification == 'Adres został opublikowany na profilu badanej osoby (x.example). ' +
                'Niezależne źródła różnego pochodzenia (forum.example, p2p.example, x.example) wskazują na ten sam adres.'
        !(attribution.justification =~ /\b\d+\b|%/)
    }

    def "no independent-source bonus for #description"() {
        when:
        def attribution = scoreOne([
            mention('m1', firstUrl, firstText),
            mention('m2', secondUrl, secondText)
        ], [NICKNAME])

        then:
        attribution.confidenceLevel == 40
        attribution.appliedRules*.rule == [
            'DISCUSSION',
            rule,
            'NICKNAME_ONLY'
        ]

        where:
        description                                  | firstUrl                    | firstText                                | secondUrl                       | secondText || rule
        'several Mentions on one service'            | 'https://forum.example/t/1' | "zenek_fx: mój adres ${BTC}."             | 'https://FORUM.example/t/2'     | "zenek_fx znowu: ${BTC}" || 'SAME_SERVICE'
        'the www and bare host of one service'       | 'https://forum.example/t/1' | "zenek_fx: mój adres ${BTC}."             | 'https://www.forum.example/t/2' | "zenek_fx znowu: ${BTC}" || 'SAME_SERVICE'
        'the same sentence copied to another service' | 'https://forum.example/t/1' | "Cześć! zenek_fx: mój BTC to ${BTC}."    | 'https://mirror.example/r/1'    | "Repost z forum. zenek_fx: Mój  BTC to ${BTC}!" || 'COPIED_TEXT'
    }

    def "an address quoted in a warning about another person gives no support"() {
        when:
        def attribution = scoreOne([
            mention('m1', 'https://forum.example/t/9', "Uwaga, oszust podszywa się pod zenek_fx i podaje adres ${BTC}. Nie wpłacajcie!")
        ], [NICKNAME])

        then:
        attribution.confidenceLevel == 0
        attribution.appliedRules*.rule == [
            'NO_PERSON_LINK',
            'WARNING_CONTEXT'
        ]
        attribution.justification == NO_LINK + ' Wzmianki, w których adres występuje w ostrzeżeniu lub z prośbą, by go nie używać, ' +
                'nie zostały uznane za potwierdzenie.'
    }

    def "a warning affects only the address in its own sentence"() {
        given:
        def mentions = [
            mention('m1', 'https://forum.example/t/9', "zenek_fx: mój BTC ${BTC}. Oszust używa adresu ${ETH_EIP55}, nie wpłacajcie.")
        ]

        when:
        def result = scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, ['m1']),
            new Candidate('ETH', ETH, ['m1'])
        ], [], mentions, [NICKNAME])).attributions

        then:
        result*.confidenceLevel == [40, 0]
        result[0].appliedRules*.rule == ['DISCUSSION', 'NICKNAME_ONLY']
        result[1].appliedRules*.rule == [
            'NO_PERSON_LINK',
            'WARNING_CONTEXT'
        ]
    }

    def "a Mention with another person's e-mail is no support when the Case has an e-mail: #description"() {
        expect:
        scoreOne([
            mention('m1', 'https://forum.example/t/5', context)
        ], identifiers).appliedRules*.rule == rules

        where:
        description                       | identifiers       | context || rules
        'only another e-mail'             | [NICKNAME, EMAIL] | "zenek_fx (jan.nowak@example.org) ma adres ${BTC}" || [
            'NO_PERSON_LINK',
            'CONFLICTING_EMAIL'
        ]
        'the person\'s e-mail is there too' | [NICKNAME, EMAIL] | "zenek_fx, zenon.fikcyjny@example.com lub jan.nowak@example.org: ${BTC}" || ['DISCUSSION']
        'the Case has no e-mail'          | [NICKNAME]        | "zenek_fx (jan.nowak@example.org) ma adres ${BTC}" || ['DISCUSSION', 'NICKNAME_ONLY']
    }

    def "a Mention found only through the Analyst's wallet gives no support and the wallet is marked as input"() {
        given:
        def mentions = [
            mention('m1', 'https://forum.example/t/7', "Wyślij na ${ETH_EIP55} albo BTC ${BTC}.")
        ]

        when:
        def result = scoring.score(new ScoringInput([
            new Candidate('ETH', ETH, ['m1']),
            new Candidate('BTC', BTC, ['m1'])
        ], [], mentions, [WALLET])).attributions

        then:
        result*.confidenceLevel == [0, 0]
        result[0].appliedRules*.rule == [
            'ENTERED_BY_ANALYST',
            'NO_PERSON_LINK'
        ]
        result[0].justification == 'Adres został wpisany przez analityka jako dane sprawy; samo wpisanie nie jest dowodem. ' + NO_LINK
        result[1].appliedRules*.rule == ['NO_PERSON_LINK']
    }

    def "the wallet is scored only from Mentions that name the person, so a wallet-only Mention adds no bonus"() {
        when:
        def attribution = scoreOne([
            mention('m1', 'https://forum.example/t/1', "zenek_fx pisze: mój ETH to ${ETH_EIP55}."),
            mention('m2', 'https://list.example/a/1', "Lista adresów: ${ETH_EIP55}.")
        ], [WALLET, NICKNAME], ETH)

        then:
        attribution.confidenceLevel == 40
        attribution.appliedRules*.rule == [
            'ENTERED_BY_ANALYST',
            'DISCUSSION',
            'NICKNAME_ONLY'
        ]
    }

    def "the full name never links an address to the person"() {
        expect:
        scoreOne([
            mention('m1', 'https://forum.example/t/1', "Zenon Fikcyjny: ${ETH_EIP55}")
        ], [WALLET, FULL_NAME], ETH).appliedRules*.rule == [
            'ENTERED_BY_ANALYST',
            'NO_PERSON_LINK'
        ]
    }

    def "an unclassified candidate is scored with the same level and a sentence about the missing check"() {
        given:
        def mentions = [
            mention('m1', 'https://forum.example/t/1', "zenek_fx: BTC ${BTC}, ETH ${ETH_EIP55}")
        ]
        def accepted = new Candidate('BTC', BTC, ['m1'])
        def unchecked = new Candidate('ETH', ETH, ['m1'])

        when:
        def result = scoring.score(new ScoringInput([accepted], [unchecked], mentions, [NICKNAME])).attributions

        then:
        result*.candidate == [accepted, unchecked]
        result*.confidenceLevel == [40, 40]
        result[1].appliedRules*.rule == [
            'DISCUSSION',
            'NICKNAME_ONLY',
            'CLASSIFICATION_UNAVAILABLE'
        ]
        result[1].justification.endsWith('Nie udało się sprawdzić, czy adres należy do giełdy, kontraktu lub innej usługi.')
    }

    def "a Mention id missing from the search result gives no support"() {
        expect:
        scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, ['gone'])
        ], [], [], [NICKNAME]))
        .attributions[0].appliedRules*.rule == ['NO_PERSON_LINK']
    }

    def "the same input always gives the same result, whatever the order of Mentions"() {
        given:
        def mentions = [
            mention('m1', 'https://x.example/zenek_fx/status/555', "@zenek_fx: BTC ${BTC}."),
            mention('m2', 'https://p2p.example/ad/987', "Ogłoszenie zenek_fx: ${BTC}."),
            mention('m3', 'https://forum.example/t/1', "zenek_fx: ${BTC}.")
        ]

        when:
        def forward = scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, ['m1', 'm2', 'm3'])
        ], [], mentions, [NICKNAME, PROFILE]))
        def again = scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, ['m1', 'm2', 'm3'])
        ], [], mentions, [NICKNAME, PROFILE]))
        def backward = scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, ['m3', 'm2', 'm1'])
        ], [], mentions.reverse(), [PROFILE, NICKNAME]))

        then:
        forward == again
        backward.attributions[0].confidenceLevel == forward.attributions[0].confidenceLevel
        backward.attributions[0].appliedRules == forward.attributions[0].appliedRules
        backward.attributions[0].justification == forward.attributions[0].justification
    }

    def "every candidate gets exactly one Confidence Level between 0 and 100, in input order"() {
        given:
        def mentions = [
            mention('m1', 'https://forum.example/t/1', "zenek_fx: ${BTC} ${ETH_EIP55}")
        ]
        def first = new Candidate('ETH', ETH, ['m1'])
        def second = new Candidate('BTC', BTC, ['m1'])

        when:
        def attributions = scoring.score(new ScoringInput([first, second], [], mentions, [NICKNAME])).attributions

        then:
        attributions*.candidate == [first, second]
        attributions.every { it.confidenceLevel in 0..100 }
        scoring.score(new ScoringInput([], [], [], [NICKNAME])).attributions.isEmpty()
    }
}
