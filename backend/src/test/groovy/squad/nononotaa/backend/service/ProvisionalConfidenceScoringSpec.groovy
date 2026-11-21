package squad.nononotaa.backend.service

import java.time.Instant
import spock.lang.Specification
import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.model.ClassificationStatus

class ProvisionalConfidenceScoringSpec extends Specification {

    static final Instant AT = Instant.parse('2026-11-21T10:00:00Z')
    static final AppliedRule BASE = new AppliedRule('MENTIONED_WITH_IDENTIFIER', 40)
    static final AppliedRule BONUS = new AppliedRule('INDEPENDENT_SOURCES', 15)

    def scoring = new ProvisionalConfidenceScoring()

    private static FoundMention mention(String id, String url, String context) {
        new FoundMention(id, 'synthetic', url, AT, context, [])
    }

    private static Candidate candidate(List<String> mentionIds) {
        new Candidate('BTC', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa', mentionIds)
    }

    def "an address seen in one Mention gets the base Confidence Level with its rule and the algorithm version"() {
        when:
        def result = scoring.score([candidate(['m1'])], [
            mention('m1', 'https://forum.example/t/1', 'my BTC')
        ])

        then:
        result.algorithmVersion == 'provisional-1'
        result.attributions.size() == 1
        result.attributions[0].confidenceLevel == 40
        result.attributions[0].appliedRules == [BASE]
        result.attributions[0].classificationStatus == ClassificationStatus.NOT_EXCLUDED
    }

    def "Mentions on two different services with their own text add the independent-source bonus"() {
        when:
        def result = scoring.score([candidate(['m1', 'm2'])], [
            mention('m1', 'https://forum.example/t/1', 'forum post with my BTC'),
            mention('m2', 'https://www.x.example/status/5', 'tweet with my BTC'),
        ])

        then:
        result.attributions[0].confidenceLevel == 55
        result.attributions[0].appliedRules == [BASE, BONUS]
    }

    def "no bonus for #description"() {
        when:
        def result = scoring.score([candidate(['m1', 'm2'])], [
            mention('m1', firstUrl, firstText),
            mention('m2', secondUrl, secondText),
        ])

        then:
        result.attributions[0].confidenceLevel == 40
        result.attributions[0].appliedRules == [BASE]

        where:
        description                              | firstUrl                    | firstText     | secondUrl                     | secondText
        'several Mentions on one service'        | 'https://forum.example/t/1' | 'first post'  | 'https://FORUM.example/t/2'   | 'second post'
        'the same text copied to another service' | 'https://forum.example/t/1' | 'my  BTC addr' | 'https://mirror.example/copy' | 'My BTC addr'
        'www and bare host of one service'       | 'https://forum.example/t/1' | 'first post'  | 'https://www.forum.example/t' | 'second post'
    }

    def "the same Mentions always give the same Confidence Level, whatever their order"() {
        given:
        def mentions = [
            mention('m1', 'https://forum.example/t/1', 'forum'),
            mention('m2', 'https://x.example/s/1', 'tweet'),
            mention('m3', 'https://forum.example/t/2', 'forum again'),
        ]

        when:
        def forward = scoring.score([candidate(['m1', 'm2', 'm3'])], mentions).attributions[0]
        def backward = scoring.score([candidate(['m3', 'm2', 'm1'])], mentions.reverse()).attributions[0]

        then:
        forward.confidenceLevel == 55
        backward.confidenceLevel == forward.confidenceLevel
        backward.appliedRules == forward.appliedRules
    }

    def "a Mention id missing from the search result gives no support beyond the base rule"() {
        expect:
        scoring.score([candidate(['m1', 'gone'])], [
            mention('m1', 'https://forum.example/t/1', 'x')
        ]).attributions[0].confidenceLevel == 40
    }

    def "every candidate gets exactly one Attribution, in input order"() {
        given:
        def first = new Candidate('ETH', '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed', ['m1'])
        def second = candidate(['m1'])

        expect:
        scoring.score([first, second], [
            mention('m1', 'https://forum.example/t/1', 'x')
        ]).attributions*.candidate == [first, second]
        scoring.score([], []).attributions.isEmpty()
    }
}
