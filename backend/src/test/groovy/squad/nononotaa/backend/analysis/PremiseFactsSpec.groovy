package squad.nononotaa.backend.analysis

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.model.AuthorMatch

class PremiseFactsSpec extends Specification {

    static final Instant AT = Instant.parse('2026-11-21T10:00:00Z')
    static final String BTC = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
    static final CaseIdentifier NICKNAME = new CaseIdentifier(IdentifierType.NICKNAME, 'zenek_fx')
    static final CaseIdentifier LOGIN = new CaseIdentifier(IdentifierType.LOGIN, 'Octo-Dev')
    static final CaseIdentifier PROFILE = new CaseIdentifier(IdentifierType.PROFILE_URL, 'https://x.example/zenek_fx')
    static final CaseIdentifier EMAIL = new CaseIdentifier(IdentifierType.EMAIL, 'zenon.fikcyjny@example.com')
    static final CaseIdentifier FULL_NAME = new CaseIdentifier(IdentifierType.FULL_NAME, 'Zenon Fikcyjny')

    private static FoundMention mention(Map args = [:]) {
        new FoundMention(
                'm1',
                args.sourceName ?: 'synthetic',
                args.url ?: 'https://forum.example/t/1',
                AT,
                null,
                args.detectedAddresses ?: [BTC],
                (String) args.author)
    }

    private static AddressWindow window(String before, String after = '') {
        new AddressWindow(BTC, BTC, before + BTC + after, before.length(), BTC.length())
    }

    private static PremiseFacts facts(FoundMention mention, AddressWindow window, List<CaseIdentifier> identifiers) {
        PremiseFactsKt.premiseFacts(mention, window, identifiers)
    }

    def "the author is the person when it equals a nickname or login, ignoring a leading @ and case: #author -> #expected"() {
        expect:
        facts(mention(author: author), window('BTC: '), identifiers).authorIsPerson == expected

        where:
        author       | identifiers || expected
        'zenek_fx'   | [NICKNAME] || AuthorMatch.YES
        '@Zenek_FX'  | [NICKNAME] || AuthorMatch.YES
        'octo-dev'   | [NICKNAME, LOGIN] || AuthorMatch.YES
        'zenek_fx'   | [
            new CaseIdentifier(IdentifierType.NICKNAME, '@zenek_fx')
        ] || AuthorMatch.YES
        'someone'    | [NICKNAME, LOGIN] || AuthorMatch.NO
        'zenek_fx2'  | [NICKNAME] || AuthorMatch.NO
        null         | [NICKNAME] || AuthorMatch.UNKNOWN
        '  '         | [NICKNAME] || AuthorMatch.UNKNOWN
        'zenek_fx'   | [PROFILE, EMAIL] || AuthorMatch.UNKNOWN
        'zenek_fx'   | [
            new CaseIdentifier(IdentifierType.NICKNAME, ' ')
        ] || AuthorMatch.UNKNOWN
    }

    def "the URL is under the profile address only on the profile itself or below it: #url -> #expected"() {
        expect:
        facts(mention(url: url), window('BTC: '), [NICKNAME, PROFILE]).underProfile == expected

        where:
        url || expected
        'https://x.example/zenek_fx' || true
        'http://www.X.example/zenek_fx/' || true
        'https://x.example/zenek_fx/status/555' || true
        'https://x.example/zenek_fx?tab=posts' || true
        'https://x.example/zenek_fx#top' || true
        'https://x.example/zenek_fx2' || false
        'https://x.example/other/zenek_fx' || false
    }

    def "without a profile address no URL is under the profile: #url"() {
        expect:
        !facts(mention(url: url), window('BTC: '), [
            NICKNAME,
            new CaseIdentifier(IdentifierType.PROFILE_URL, ' ')
        ]).underProfile

        where:
        url << [
            'https://x.example/zenek_fx',
            '/zenek_fx'
        ]
    }

    def "the number of addresses counts distinct detected addresses, invalid ones included"() {
        expect:
        facts(mention(detectedAddresses: [BTC, 'bc1qinvalid', BTC]), window('BTC: '), [NICKNAME]).addressCount == 2
    }

    def "the host is the lower-case URL host without www, null when the URL cannot be parsed: #url -> #expected"() {
        expect:
        facts(mention(url: url), window('BTC: '), [NICKNAME]).host == expected

        where:
        url || expected
        'https://www.Forum.example/t/1' || 'forum.example'
        'https://bitcointalk.org/index.php' || 'bitcointalk.org'
        'not a url' || null
        'https://exa mple.com/x' || null
    }

    def "a nickname occurs in the window only as a whole word, ignoring a leading @ and case: '#text' -> #found"() {
        expect:
        facts(mention(), window(text + ' BTC: '), identifiers).identifierTypes == (found ? [IdentifierType.NICKNAME] : [])

        where:
        text                    | identifiers || found
        'pisze @zenek_fx'       | [NICKNAME] || true
        'zenek_fx: mój adres'   | [NICKNAME] || true
        'od ZENEK_FX'           | [NICKNAME] || true
        '(zenek_fx)'            | [NICKNAME] || true
        'zenek_fx2 pisze'       | [NICKNAME] || false
        'xzenek_fx pisze'       | [NICKNAME] || false
        '_zenek_fx pisze'       | [NICKNAME] || false
        'zenek pisze'           | [NICKNAME] || false
        'the maximum is'        | [
            new CaseIdentifier(IdentifierType.NICKNAME, 'max')
        ] || false
        'max wrote'             | [
            new CaseIdentifier(IdentifierType.NICKNAME, 'max')
        ] || true
        'max wrote'             | [
            new CaseIdentifier(IdentifierType.NICKNAME, '@max')
        ] || true
        'max wrote'             | [
            new CaseIdentifier(IdentifierType.NICKNAME, ' ')
        ] || false
    }

    def "identifiers are matched without diacritics, like the keyword lists: '#text' vs '#value' -> #found"() {
        expect:
        facts(mention(), window(text + ' BTC: '), [
            new CaseIdentifier(IdentifierType.NICKNAME, value)
        ]).identifierTypes == (found ? [IdentifierType.NICKNAME] : [])

        where:
        text                        | value || found
        'od ZAZOLC'                 | 'zażółć' || true
        'od Zażółć'                 | 'zazolc' || true
        'od Zażółć' | 'zażółć' || true
        'od zażółćx'                | 'zażółć' || false
        'od Łukasz'                 | 'lukasz' || true
    }

    def "an e-mail occurs only as a whole e-mail address: '#text' -> #found"() {
        expect:
        facts(mention(), window(text + ' BTC: '), [EMAIL]).identifierTypes == (found ? [IdentifierType.EMAIL] : [])

        where:
        text || found
        'napisz: Zenon.Fikcyjny@Example.com' || true
        '<zenon.fikcyjny@example.com>' || true
        'a.zenon.fikcyjny@example.com' || false
        'zenon.fikcyjny@example.com.evil.pl' || false
    }

    def "a profile address occurs in the window without its scheme and www: '#text' -> #found"() {
        expect:
        facts(mention(), window(text + ' BTC: '), [PROFILE]).identifierTypes == (found ? [IdentifierType.PROFILE_URL] : [])

        where:
        text || found
        'see https://x.example/zenek_fx/' || true
        'see www.X.example/zenek_fx/status/1' || true
        'see x.example/zenek_fx2' || false
    }

    def "identifier types are distinct and sorted, include the full name and the messenger ID, never the wallet address"() {
        given:
        def identifiers = [
            FULL_NAME,
            new CaseIdentifier(IdentifierType.WALLET_ADDRESS, BTC),
            EMAIL,
            new CaseIdentifier(IdentifierType.MESSENGER_ID, 'tg:zenek77'),
            new CaseIdentifier(IdentifierType.NICKNAME, 'zenek'),
            NICKNAME,
            LOGIN,
            PROFILE,
        ]
        def text = 'Zenon Fikcyjny (zenek_fx, zenek, Octo-Dev, tg:zenek77) zenon.fikcyjny@example.com x.example/zenek_fx BTC: '

        expect:
        facts(mention(), window(text), identifiers).identifierTypes == [
            IdentifierType.NICKNAME,
            IdentifierType.LOGIN,
            IdentifierType.EMAIL,
            IdentifierType.MESSENGER_ID,
            IdentifierType.PROFILE_URL,
            IdentifierType.FULL_NAME,
        ]
    }

    def "a run of white space matches one space, like the keyword lists"() {
        expect:
        facts(mention(), window('od Zenon\n   Fikcyjny BTC: '), [FULL_NAME]).identifierTypes == [IdentifierType.FULL_NAME]
    }

    def "an identifier only in the URL or the author is not in the window"() {
        expect:
        facts(mention(url: 'https://x.example/zenek_fx', author: 'zenek_fx'), window('BTC: '), [NICKNAME, PROFILE]).identifierTypes == []
    }

    static final String DECOMPOSED = 'Zażółć'

    def "the identifier distance is the gap in characters of the window between the nearest linking identifier and the address: #label"() {
        expect:
        facts(mention(), new AddressWindow(BTC, BTC, before + BTC + after, before.length(), BTC.length()), identifiers).identifierDistance == expected

        where:
        label                               | before                                  | after                       | identifiers || expected
        'about 400 characters before'       | 'zenek_fx' + ' x' * 200                 | ''                          | [NICKNAME] || 400
        'right before'                      | 'zenek_fx:'                             | ''                          | [NICKNAME] || 1
        'right after'                       | ''                                      | ':zenek_fx'                 | [NICKNAME] || 1
        'after, past the address'           | 'BTC '                                  | ' ' * 30 + 'zenek_fx'       | [NICKNAME] || 30
        'the nearest of several'            | 'zenek_fx' + ' x' * 200                 | ' Octo-Dev'                 | [NICKNAME, LOGIN] || 1
        'the nearest occurrence of one'     | 'zenek_fx' + ' x' * 200 + ' zenek_fx '  | ''                          | [NICKNAME] || 1
        'an e-mail'                         | 'zenon.fikcyjny@example.com, '          | ''                          | [EMAIL] || 2
        'a profile address'                 | 'https://x.example/zenek_fx  '          | ''                          | [PROFILE] || 2
        'a messenger ID'                    | 'tg:zenek77 '                           | ''                          | [
            new CaseIdentifier(IdentifierType.MESSENGER_ID, 'tg:zenek77')
        ] || 1
        'diacritics before the identifier'  | DECOMPOSED + '   zenek_fx' + ' x' * 10  | ''                          | [NICKNAME] || 20
        'diacritics in the gap'             | 'zenek_fx ' + DECOMPOSED * 10 + '   '   | ''                          | [NICKNAME] || 1 + DECOMPOSED.length() * 10 + 3
        'no identifier in the window'       | 'BTC: '                                 | ''                          | [NICKNAME, EMAIL] || null
        'only the full name'                | 'Zenon Fikcyjny '                       | ''                          | [FULL_NAME, NICKNAME] || null
        'only the wallet address'           | ''                                      | ''                          | [
            new CaseIdentifier(IdentifierType.WALLET_ADDRESS, BTC)
        ] || null
    }

    def "an e-mail of another person in the window conflicts only when the Case has an e-mail and none in the window is it: #label"() {
        expect:
        facts(mention(), window(text + ' BTC: '), identifiers).conflictingEmail == expected

        where:
        label                               | text                                                        | identifiers || expected
        'another e-mail'                    | 'kontakt: anna.inna@example.org'                            | [NICKNAME, EMAIL] || true
        'the Case e-mail in another case'   | 'kontakt: Zenon.Fikcyjny@EXAMPLE.com'                       | [EMAIL] || false
        'the Case e-mail and another'       | 'zenon.fikcyjny@example.com, anna.inna@example.org'         | [EMAIL] || false
        'a longer e-mail around the Case one' | 'a.zenon.fikcyjny@example.com'                            | [EMAIL] || true
        'no e-mail in the window'           | 'zenek_fx pisze'                                            | [EMAIL] || false
        'no e-mail in the Case'             | 'kontakt: anna.inna@example.org'                            | [NICKNAME] || false
        'a blank Case e-mail'               | 'kontakt: anna.inna@example.org'                            | [
            new CaseIdentifier(IdentifierType.EMAIL, ' ')
        ] || false
    }

    def "an e-mail of another person outside the window does not conflict"() {
        given:
        def outside = new FoundMention('m1', 'synthetic', 'https://forum.example/t/1', AT, 'kontakt: anna.inna@example.org', [BTC], null)

        expect:
        !facts(outside, window('BTC: '), [EMAIL]).conflictingEmail
    }

    def "the source name is the Mention's source"() {
        expect:
        facts(mention(sourceName: 'github'), window('BTC: '), [NICKNAME]).sourceName == 'github'
    }
}
