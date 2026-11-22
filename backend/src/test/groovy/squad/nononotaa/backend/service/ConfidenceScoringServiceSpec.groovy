package squad.nononotaa.backend.service

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import spock.lang.Specification
import squad.nononotaa.backend.analysis.AddressCandidateDetector
import squad.nononotaa.backend.analysis.AddressWindow
import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.KindProbabilities
import squad.nononotaa.backend.analysis.PremiseAnswers
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.ScoringInput
import squad.nononotaa.backend.analysis.YesNoProbabilities
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.AssessedBy
import squad.nononotaa.backend.model.AssessmentMode
import squad.nononotaa.backend.model.AuthorMatch
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.DismissalReason
import squad.nononotaa.backend.model.Mention
import squad.nononotaa.backend.model.ModelQuestion
import squad.nononotaa.backend.model.PremiseKind
import squad.nononotaa.backend.model.PremiseStatus
import squad.nononotaa.backend.repository.MentionRepository

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

    static final AddressCandidateDetector DETECTOR = new AddressCandidateDetector()
    static final AddressValidator VALIDATOR = new AddressValidator()

    def scoring = new ConfidenceScoringService()

    /** A Mention whose windows come from the production detector, with the canonical address filled like the search does. */
    private static FoundMention mention(String id, String url, String content, String author = null) {
        def windows = DETECTOR.windows(content).collect { AddressWindow w ->
            def validation = VALIDATOR.validate(w.rawAddress, null)
            def canonical = validation instanceof AddressValidation.Valid ? validation.canonicalAddress.address : null
            new AddressWindow(w.rawAddress, canonical, w.text, w.addressStart, w.addressLength)
        }
        new FoundMention(id, 'synthetic', url, AT, content, windows*.rawAddress, author, windows)
    }

    private assess(FoundMention mention, List<CaseIdentifier> identifiers, int window = 0) {
        scoring.assessPremise(mention, mention.addressWindows[window], identifiers)
    }

    def "a post by the person links the address even without the nickname in the text"() {
        when:
        def premise = assess(mention('m1', 'https://bitcointalk.example/index.php?topic=1', "Sprzedam koparkę, płatność na ${BTC}", 'zenek_fx'), [NICKNAME])

        then:
        premise.id == "m1:${BTC}"
        premise.mentionId == 'm1'
        premise.address == BTC
        premise.facts.authorIsPerson == AuthorMatch.YES
        premise.status == PremiseStatus.COUNTED
        premise.kind == PremiseKind.SALE_OR_SETTLEMENT
        premise.points == 60
        premise.dismissalReason == null
        premise.assessedBy == AssessedBy.RULE
    }

    def "the nickname links only as a whole word: #text"() {
        expect:
        with(assess(mention('m1', 'https://forum.example/t/1', "${text} pisze: mój adres to ${BTC}."), [NICKNAME])) {
            status == expectedStatus
            dismissalReason == reason
            points == level
        }

        where:
        text || expectedStatus          | reason                         | level
        '@zenek_fx' || PremiseStatus.COUNTED   | null                           | 40
        'zenek_fx:' || PremiseStatus.COUNTED   | null                           | 40
        'ZENEK_FX' || PremiseStatus.COUNTED   | null                           | 40
        'zenek_fx2' || PremiseStatus.DISMISSED | DismissalReason.NO_PERSON_LINK | 0
        'xzenek_fx' || PremiseStatus.DISMISSED | DismissalReason.NO_PERSON_LINK | 0
    }

    def "a nickname inside a longer word is no link: max in maximum"() {
        expect:
        with(assess(mention('m1', 'https://forum.example/t/fees', "maximum fee, send to ${BTC}"), [
            new CaseIdentifier(IdentifierType.NICKNAME, 'max')
        ])) {
            status == PremiseStatus.DISMISSED
            dismissalReason == DismissalReason.NO_PERSON_LINK
            kind == null
            points == 0
        }
    }

    def "an identifier more than 200 characters from the address is no link, and the distance is still recorded"() {
        when:
        def far = assess(mention('m1', 'https://forum.example/t/1', "zenek_fx ${'a' * 250} ${BTC}"), [NICKNAME])
        def near = assess(mention('m1', 'https://forum.example/t/1', "zenek_fx ${'a' * 190} ${BTC}"), [NICKNAME])

        then:
        far.facts.identifierDistance == 252
        far.status == PremiseStatus.DISMISSED
        far.dismissalReason == DismissalReason.NO_PERSON_LINK
        near.facts.identifierDistance == 192
        near.status == PremiseStatus.COUNTED
    }

    def "the nickname in the URL alone is not a text link"() {
        expect:
        assess(mention('m1', 'https://forum.example/u/zenek_fx/post/1', "Mój adres: ${BTC}"), [NICKNAME]).dismissalReason ==
        DismissalReason.NO_PERSON_LINK
    }

    def "a page under the profile is the own profile only when the author is the person or unknown: #description"() {
        expect:
        with(assess(mention('m1', 'https://x.example/zenek_fx/status/1', "Napiwki: ${BTC}", author), [NICKNAME, PROFILE])) {
            status == expectedStatus
            kind == expectedKind
            points == level
            dismissalReason == reason
        }

        where:
        description      | author || expectedStatus          | expectedKind            | level | reason
        'the person'     | 'zenek_fx' || PremiseStatus.COUNTED   | PremiseKind.OWN_PROFILE | 80    | null
        'unknown'        | null || PremiseStatus.COUNTED   | PremiseKind.OWN_PROFILE | 80    | null
        'another person' | 'janek99' || PremiseStatus.DISMISSED | null                    | 0     | DismissalReason.NO_PERSON_LINK
    }

    def "a linked Premise in a warning sentence is for review with no points"() {
        expect:
        with(assess(mention('m1', 'https://forum.example/t/9', "Uwaga, oszust podszywa się pod zenek_fx i podaje adres ${BTC}. Nie wpłacajcie!"), [NICKNAME])) {
            status == PremiseStatus.FOR_REVIEW
            dismissalReason == DismissalReason.WARNING
            kind == null
            points == 0
        }
    }

    def "a Premise without a link stays dismissed for no link even in a warning sentence"() {
        expect:
        assess(mention('m1', 'https://forum.example/t/9', "Uwaga, oszust podaje adres ${BTC}. Nie wpłacajcie!"), [NICKNAME]).dismissalReason ==
        DismissalReason.NO_PERSON_LINK
    }

    def "a warning outside the address sentence does not touch the Premise"() {
        expect:
        assess(mention('m1', 'https://forum.example/t/9', "zenek_fx: mój adres ${BTC}. Uwaga na oszustów!"), [NICKNAME]).status ==
        PremiseStatus.COUNTED
    }

    def "another person's e-mail in the window dismisses a linked Premise, before the warning is read"() {
        expect:
        with(assess(mention('m1', 'https://forum.example/t/5', "zenek_fx (jan.nowak@example.org), oszust, ma adres ${BTC}."), [NICKNAME, EMAIL])) {
            status == PremiseStatus.DISMISSED
            dismissalReason == DismissalReason.CONFLICTING_EMAIL
            points == 0
        }
    }

    def "a Premise without a link is dismissed for no link before the e-mail is read"() {
        expect:
        assess(mention('m1', 'https://forum.example/t/5', "Kontakt jan.nowak@example.org, adres ${BTC}."), [NICKNAME, EMAIL]).dismissalReason ==
        DismissalReason.NO_PERSON_LINK
    }

    def "the kind comes from the keyword lists on the URL and the window: #expectedKind"() {
        expect:
        with(assess(mention('m1', url, text), [NICKNAME])) {
            kind == expectedKind
            points == level
        }

        where:
        url                          | text || expectedKind                   | level
        'https://p2p.example/ad/1'   | "Od zenek_fx. Adres: ${BTC}." || PremiseKind.SALE_OR_SETTLEMENT | 60
        'https://social.example/p/9' | "GIVEAWAY! zenek_fx: ${BTC}" || PremiseKind.MASS_COMMENT       | 10
        'https://forum.example/t/3'  | "zenek_fx pisze: mój adres to ${BTC}." || PremiseKind.DISCUSSION         | 40
    }

    private ScoredAttribution scoreOne(List<FoundMention> mentions, List<CaseIdentifier> identifiers, String address = BTC) {
        def network = address.startsWith('0x') ? 'ETH' : 'BTC'
        scoring.score(new ScoringInput([
            new Candidate(network, address, mentions*.id)
        ], [], mentions, identifiers)).attributions[0]
    }

    private static String id(String mentionId, String address = BTC) {
        "${mentionId}:${address}"
    }

    def "an address on the person's own profile gets the own-profile base, its rule with the Premise, a Justification sentence and the algorithm version"() {
        when:
        def result = scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, ['m1'])
        ], [], [
            mention('m1', 'https://x.example/zenek_fx/status/555', "@zenek_fx: trust wallet — BTC ${BTC}. DM open.")
        ], [NICKNAME, PROFILE]))

        then:
        result.algorithmVersion == 'confidence-2'
        result.attributions.size() == 1
        with(result.attributions[0]) {
            confidenceLevel == 80
            appliedRules == [
                new AppliedRule('OWN_PROFILE', 80, [id('m1')])
            ]
            premises*.id == [id('m1')]
            premises[0].kind == PremiseKind.OWN_PROFILE
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
        attribution.appliedRules*.premiseIds == [[id('m1')], [id('m1')]]
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
            mention('m1', 'https://github.com/zenek_fx', context, author)
        ], identifiers)

        then:
        attribution.appliedRules*.rule == rules
        attribution.confidenceLevel == level

        where:
        description                   | identifiers           | author    | context || rules                           | level
        'e-mail'                      | [NICKNAME, EMAIL]     | null      | "zenek_fx, kontakt zenon.fikcyjny@example.com, BTC ${BTC}" || ['OWN_PROFILE']                 | 80
        'e-mail far from the address' | [NICKNAME, EMAIL]     | null      | "zenek_fx, BTC ${BTC} ${'a' * 300} zenon.fikcyjny@example.com" || ['OWN_PROFILE']                 | 80
        'full name'                   | [NICKNAME, FULL_NAME] | null      | "Zenon Fikcyjny (zenek_fx), BTC ${BTC}" || ['OWN_PROFILE']                 | 80
        'nickname alone'              | [NICKNAME]            | null      | "zenek_fx, BTC ${BTC}" || ['DISCUSSION', 'NICKNAME_ONLY'] | 40
        'e-mail, another author'      | [NICKNAME, EMAIL]     | 'janek99' | "zenek_fx, kontakt zenon.fikcyjny@example.com, BTC ${BTC}" || ['DISCUSSION']                  | 40
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
            new AppliedRule('OWN_PROFILE', 80, [id('m1')]),
            new AppliedRule('INDEPENDENT_SOURCES', 15, [id('m1'), id('m2'), id('m3')])
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
        attribution.appliedRules*.premiseIds == [
            [id('m1'), id('m2')],
            [id('m1'), id('m2')],
            [id('m1'), id('m2')]
        ]

        where:
        description                                  | firstUrl                    | firstText                                | secondUrl                       | secondText || rule
        'several Mentions on one service'            | 'https://forum.example/t/1' | "zenek_fx: mój adres ${BTC}."             | 'https://FORUM.example/t/2'     | "zenek_fx znowu: ${BTC}" || 'SAME_SERVICE'
        'the www and bare host of one service'       | 'https://forum.example/t/1' | "zenek_fx: mój adres ${BTC}."             | 'https://www.forum.example/t/2' | "zenek_fx znowu: ${BTC}" || 'SAME_SERVICE'
        'the same sentence copied to another service' | 'https://forum.example/t/1' | "Cześć! zenek_fx: mój BTC to ${BTC}."    | 'https://mirror.example/r/1'    | "Repost z forum. zenek_fx: Mój  BTC to ${BTC}!" || 'COPIED_TEXT'
    }

    def "a mass comment never takes part in the independent-source bonus: #description"() {
        when:
        def attribution = scoreOne([
            mention('m1', 'https://forum.example/t/1', firstText),
            mention('m2', 'https://social.example/post/1', secondText)
        ], [NICKNAME])

        then:
        attribution.confidenceLevel == level
        attribution.appliedRules*.rule == rules
        attribution.appliedRules.find {
            it.rule == 'INDEPENDENT_SOURCES'
        }?.premiseIds == bonusIds

        where:
        description                                    | firstText                     | secondText || level | rules | bonusIds
        'a discussion and a mass comment'              | "zenek_fx: mój adres ${BTC}" | "Wielki giveaway! zenek_fx odeśle podwójnie na ${BTC}" || 40 | ['DISCUSSION', 'NICKNAME_ONLY'] | null
        'two discussions on different services'        | "zenek_fx: mój adres ${BTC}" | "Pisze zenek_fx, że jego portfel to ${BTC}" || 55 | [
            'DISCUSSION',
            'INDEPENDENT_SOURCES',
            'NICKNAME_ONLY'
        ] | [id('m1'), id('m2')]
    }

    def "a warning about the address puts the linked Premise aside for review"() {
        when:
        def attribution = scoreOne([
            mention('m1', 'https://forum.example/t/9', "Uwaga, oszust podszywa się pod zenek_fx i podaje adres ${BTC}. Nie wpłacajcie!")
        ], [NICKNAME])

        then:
        attribution.confidenceLevel == 0
        attribution.appliedRules == [
            new AppliedRule('WARNING_CONTEXT', 0, [id('m1')])
        ]
        attribution.premises*.status == [PremiseStatus.FOR_REVIEW]
        attribution.justification == 'Wzmianki, w których adres występuje w ostrzeżeniu lub z prośbą, by go nie używać, ' +
                'wymagają weryfikacji, więc nie zostały uznane za potwierdzenie.'
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
        result[1].appliedRules == [
            new AppliedRule('WARNING_CONTEXT', 0, [id('m1', ETH)])
        ]
    }

    def "no-link lists only the Premises without a link and is left out when every Premise was put aside: #description"() {
        expect:
        scoreOne(mentions, [NICKNAME, EMAIL]).appliedRules == rules

        where:
        description                     | mentions || rules
        'a warning and no link'         | [
            mention('m1', 'https://forum.example/t/9', "zenek_fx: oszust podaje adres ${BTC}."),
            mention('m2', 'https://list.example/a/1', "Lista adresów: ${BTC}.")
        ] || [
            new AppliedRule('NO_PERSON_LINK', 0, [id('m2')]),
            new AppliedRule('WARNING_CONTEXT', 0, [id('m1')])
        ]
        'a warning and another e-mail'  | [
            mention('m1', 'https://forum.example/t/9', "zenek_fx: oszust podaje adres ${BTC}."),
            mention('m2', 'https://forum.example/t/5', "zenek_fx (jan.nowak@example.org) ma adres ${BTC}")
        ] || [
            new AppliedRule('WARNING_CONTEXT', 0, [id('m1')]),
            new AppliedRule('CONFLICTING_EMAIL', 0, [id('m2')])
        ]
    }

    def "a Mention with another person's e-mail is no support when the Case has an e-mail: #description"() {
        expect:
        scoreOne([
            mention('m1', 'https://forum.example/t/5', context)
        ], identifiers).appliedRules*.rule == rules

        where:
        description                       | identifiers       | context || rules
        'only another e-mail'             | [NICKNAME, EMAIL] | "zenek_fx (jan.nowak@example.org) ma adres ${BTC}" || ['CONFLICTING_EMAIL']
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
        result[0].appliedRules == [
            new AppliedRule('ENTERED_BY_ANALYST', 0, []),
            new AppliedRule('NO_PERSON_LINK', 0, [id('m1', ETH)])
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
        attribution.premises*.status == [
            PremiseStatus.COUNTED,
            PremiseStatus.DISMISSED
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

    def "an unclassified candidate at #base is cut to 59 by the classification cap, and the breakdown still sums to the level"() {
        given:
        def mentions = [
            mention('m1', url, "zenek_fx: BTC ${BTC}"),
            mention('m2', 'https://forum.example/t/1', "Pisze zenek_fx, że jego portfel to ${BTC}")
        ]
        def candidate = new Candidate('BTC', BTC, ids)

        when:
        def attribution = scoring.score(new ScoringInput([], [candidate], mentions, [NICKNAME, PROFILE])).attributions[0]

        then:
        attribution.confidenceLevel == 59
        attribution.appliedRules.takeRight(2) == [
            new AppliedRule('CLASSIFICATION_UNAVAILABLE', 0, []),
            new AppliedRule('CLASSIFICATION_CAP', cut, [])
        ]
        attribution.appliedRules*.contribution.sum() == 59
        attribution.justification.endsWith('Nie udało się sprawdzić, czy adres należy do giełdy, kontraktu lub innej usługi. ' +
                'Z tego powodu stopień pewności ograniczono, tak aby nie wskazywał wysokiego prawdopodobieństwa.')

        where:
        base | url                                     | ids || cut
        80   | 'https://x.example/zenek_fx/status/555' | ['m1'] || -21
        95   | 'https://x.example/zenek_fx/status/555' | ['m1', 'm2'] || -36
        60   | 'https://p2p.example/ad/1'              | ['m1'] || -1
    }

    def "an unclassified candidate at 40 is scored with the same level, a sentence about the missing check and no cap"() {
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

    def "a Mention id missing from the search result, or a Mention without a window for the address, gives no Premise and no support"() {
        given:
        def stored = new FoundMention('old', 'synthetic', 'https://forum.example/t/1', AT, "zenek_fx: ${BTC}", [BTC])

        when:
        def attribution = scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, ['gone', 'old'])
        ], [], [stored], [NICKNAME])).attributions[0]

        then:
        attribution.appliedRules == [
            new AppliedRule('NO_PERSON_LINK', 0, [])
        ]
        attribution.premises.isEmpty()
    }

    def "an address written twice in one Mention gives one Premise, matched to the first window of its canonical address"() {
        given:
        def text = "zenek_fx: ETH ${ETH_EIP55}. ${'a' * 700} Raz jeszcze ${ETH}."
        def m = mention('m1', 'https://forum.example/t/1', text)

        when:
        def attribution = scoreOne([m], [NICKNAME], ETH)

        then:
        m.addressWindows.size() == 2
        attribution.premises.size() == 1
        attribution.premises[0].id == id('m1', ETH)
        attribution.premises[0].window.rawAddress == ETH_EIP55
        attribution.premises[0].status == PremiseStatus.COUNTED
    }

    def "every rule names Premises of its own Attribution"() {
        when:
        def result = scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, ['m1', 'm2', 'm3'])
        ], [], [
            mention('m1', 'https://x.example/zenek_fx/status/555', "@zenek_fx: BTC ${BTC}."),
            mention('m2', 'https://p2p.example/ad/987', "Ogłoszenie zenek_fx: ${BTC}."),
            mention('m3', 'https://forum.example/t/9', "zenek_fx: oszust podaje ${BTC}.")
        ], [NICKNAME, PROFILE]))

        then:
        with(result.attributions[0]) {
            appliedRules.every { rule ->
                rule.premiseIds.every {
                    it in premises*.id
                }
            }
            premises*.id == [id('m1'), id('m2'), id('m3')]
            appliedRules*.rule == [
                'OWN_PROFILE',
                'INDEPENDENT_SOURCES',
                'WARNING_CONTEXT'
            ]
        }
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
        backward.attributions[0].premises == forward.attributions[0].premises
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

    def "golden set #id scores as expected for confidence-2"() {
        given:
        def saved = new AtomicInteger()
        def repository = Stub(MentionRepository) {
            findByIdentity(_) >> []
            save(_) >> { Mention m ->
                GoldenSet.withId(m, "${id}-${saved.incrementAndGet()}")
            }
        }

        expect:
        GoldenSet.score(item, repository, scoring) == item.expected['confidence-2'].attributions

        where:
        item << GoldenSet.items()
        id = item.id
    }

    def "golden set #id scores as expected for confidence-4 with canned model answers"() {
        given:
        def saved = new AtomicInteger()
        def repository = Stub(MentionRepository) {
            findByIdentity(_) >> []
            save(_) >> { Mention m ->
                GoldenSet.withId(m, "${id}-${saved.incrementAndGet()}")
            }
        }
        // confidence-4 changed only the rows that have their own entry; the others keep the confidence-3 result
        def expected = (item.expected['confidence-4'] ?: item.expected['confidence-3']).attributions as List<Map>

        when:
        def result = GoldenSet.run(item, repository, scoring, AssessmentMode.WITH_MODEL)

        then:
        result.algorithmVersion == 'confidence-4'
        GoldenSet.banded(result.attributions as List<Map>, expected) == expected

        where:
        item << GoldenSet.items()
        id = item.id
    }

    def "the model's #question at #value is read as #reading"() {
        given:
        def m = mention('m1', 'https://forum.example/t/1', "zenek_fx pisze: mój adres to ${BTC}.", author)
        def kind = question == 'rodzaj_wpisu' ? new KindProbabilities(0.1, value, 0.8 - value, 0.1) : new KindProbabilities(0.05, 0.05, 0.85, 0.05)
        def answers = new PremiseAnswers(kind, yesNo(question == 'deklaracja' ? value : 0), yesNo(question == 'ostrzezenie' ? value : 0))

        when:
        def premise = scoreWithModel([m], [(id('m1')): answers]).premises[0]

        then:
        premise.assessedBy == AssessedBy.MODEL
        premise.status == status
        premise.dismissalReason == reason
        premise.undetermined == undetermined
        premise.points == points

        where:
        question      | value | author || reading        | status                   | reason                                     | undetermined                   | points
        'deklaracja'  | 0.7   | 'janek99' || 'yes'          | PremiseStatus.DISMISSED  | DismissalReason.DECLARED_BY_ANOTHER_PERSON | []                             | 0
        'deklaracja'  | 0.69  | 'janek99' || 'undetermined' | PremiseStatus.FOR_REVIEW | null                                       | [ModelQuestion.DECLARATION]    | 40
        'deklaracja'  | 0.31  | 'janek99' || 'undetermined' | PremiseStatus.FOR_REVIEW | null                                       | [ModelQuestion.DECLARATION]    | 40
        'deklaracja'  | 0.3   | 'janek99' || 'no'           | PremiseStatus.COUNTED    | null                                       | []                             | 40
        'ostrzezenie' | 0.7   | 'zenek_fx' || 'yes'          | PremiseStatus.DISMISSED  | DismissalReason.WARNING                    | []                             | 0
        'ostrzezenie' | 0.69  | 'zenek_fx' || 'undetermined' | PremiseStatus.FOR_REVIEW | null                                       | [ModelQuestion.WARNING]        | 40
        'ostrzezenie' | 0.31  | 'zenek_fx' || 'undetermined' | PremiseStatus.FOR_REVIEW | null                                       | [ModelQuestion.WARNING]        | 40
        'ostrzezenie' | 0.3   | 'zenek_fx' || 'no'           | PremiseStatus.COUNTED    | null                                       | []                             | 40
        'rodzaj_wpisu'| 0.5   | 'zenek_fx' || 'sale'         | PremiseStatus.COUNTED    | null                                       | []                             | 60
        'rodzaj_wpisu'| 0.49  | 'zenek_fx' || 'undetermined' | PremiseStatus.FOR_REVIEW | null                                       | [ModelQuestion.KIND]           | 40
    }

    def "an own declaration by the model with an undetermined declaration is for review at most as discussion, author #author"() {
        given:
        def m = mention('m1', 'https://x.example/zenek_fx/status/555', "@zenek_fx: trust wallet — BTC ${BTC}. DM open.", author)
        def answers = new PremiseAnswers(new KindProbabilities(0.75, 0.1, 0.1, 0.05), yesNo(0.44), yesNo(0.05))

        when:
        def premise = scoreWithModel([m], [(id('m1')): answers]).premises[0]

        then:
        premise.status == PremiseStatus.FOR_REVIEW
        premise.kind == PremiseKind.DISCUSSION
        premise.points == 40
        premise.undetermined == [ModelQuestion.DECLARATION]
        premise.dismissalReason == null

        where:
        author << [null, 'zenek_fx']
    }

    def "with the model on, a Premise without an answer falls back to the rules and the others keep the model"() {
        given:
        def mentions = [
            mention('m1', 'https://forum.example/t/1', "zenek_fx pisze: mój adres to ${BTC}."),
            mention('m2', 'https://social.example/p/2', "Adres zenek_fx: ${BTC}")
        ]
        def answers = new PremiseAnswers(new KindProbabilities(0.8, 0.1, 0.05, 0.05), yesNo(0.9), yesNo(0.05))

        when:
        def result = scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, ['m1', 'm2'])
        ], [], mentions, [NICKNAME], [(id('m1')): answers], AssessmentMode.WITH_MODEL))
        def premises = result.attributions[0].premises

        then:
        result.algorithmVersion == 'confidence-4'
        premises*.id == [id('m1'), id('m2')]
        premises*.assessedBy == [
            AssessedBy.MODEL,
            AssessedBy.RULE
        ]
        premises*.answers == [answers, null]
        result.attributions[0].appliedRules.find {
            it.rule == 'MODEL_ASSISTED'
        }.premiseIds == [id('m1')]
    }

    private ScoredAttribution scoreWithModel(List<FoundMention> mentions, Map<String, PremiseAnswers> answers) {
        scoring.score(new ScoringInput([
            new Candidate('BTC', BTC, mentions*.id)
        ], [], mentions, [NICKNAME], answers, AssessmentMode.WITH_MODEL)).attributions[0]
    }

    private static YesNoProbabilities yesNo(Number yes) {
        new YesNoProbabilities(yes as double, 1 - (yes as double))
    }

    def "golden set #id address #placeholder is a valid address"() {
        expect:
        new AddressValidator().validate(address, null) instanceof AddressValidation.Valid

        where:
        [id, placeholder, address]<< GoldenSet.addresses()
    }
}
