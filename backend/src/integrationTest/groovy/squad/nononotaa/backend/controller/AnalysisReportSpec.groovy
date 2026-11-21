package squad.nononotaa.backend.controller

import groovy.json.JsonSlurper
import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.AnalysisResult
import squad.nononotaa.backend.analysis.AnalysisStage
import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.ExcludedCandidate
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.RejectedCandidate
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.SourceReport
import squad.nononotaa.backend.analysis.StageFailure
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.ReportLimitation
import squad.nononotaa.backend.model.SourceStatus
import squad.nononotaa.backend.repository.AnalysisRepository

class AnalysisReportSpec extends BaseSpec {

    static final Instant AT = Instant.parse('2026-11-21T10:00:00Z')
    static final String ETH_EIP55 = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'
    static final String ETH = ETH_EIP55.toLowerCase()
    static final String ETH_BAD_CHECKSUM = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAEd'
    static final String BTC = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
    static final String KRAKEN = '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0'
    static final String SALE = 'Adres podano w ogłoszeniu sprzedaży lub w kontekście rozliczenia (p2p.example).'
    static final String ETH_JUSTIFICATION = SALE + ' Niezależne źródła różnego pochodzenia (forum.example, p2p.example) wskazują na ten sam adres.'
    static final String BTC_JUSTIFICATION = SALE + ' Nie udało się sprawdzić, czy adres należy do giełdy, kontraktu lub innej usługi.'

    @Autowired
    AnalysisRecorder recorder

    @Autowired
    AnalysisRepository repository

    String caseId = "case-${UUID.randomUUID()}"

    def forum = new FoundMention('m-forum', 'synthetic', 'https://forum.example/t/1', AT, 'zenek_fx: my ETH ' + ETH_EIP55, [ETH_EIP55, ETH_BAD_CHECKSUM])
    def market = new FoundMention('m-market', 'synthetic', 'https://p2p.example/ad/2', AT.plusSeconds(60), 'zenek_fx pays to ' + KRAKEN + ' and ' + BTC, [KRAKEN, BTC])

    private String begin() {
        recorder.begin(new AnalysisRequest(caseId, AnalysisTrigger.MANUAL, [
            new CaseIdentifier(IdentifierType.NICKNAME, 'zenek_fx')
        ]))
    }

    private String partial() {
        def id = begin()
        recorder.finish(id, new AnalysisResult(AnalysisStatus.PARTIAL, [
            new StageFailure(AnalysisStage.EXCLUSION, "CLASSIFICATION_UNAVAILABLE:${BTC}".toString())
        ],
        [
            ReportLimitation.CLASSIFICATION_UNAVAILABLE,
            ReportLimitation.ANALYSIS_INCOMPLETE
        ], [
            new SourceReport('synthetic', SourceStatus.CHECKED, true, AT)
        ],
        [forum, market], [
            new RejectedCandidate('m-forum', ETH_BAD_CHECKSUM, 'CHECKSUM_MISMATCH')
        ],
        [
            new ExcludedCandidate(new Candidate('ETH', KRAKEN, ['m-market']), ExclusionReason.EXCHANGE, 'Own label list: Kraken')
        ],
        [
            new Candidate('BTC', BTC, ['m-market'])
        ],
        [
            new ScoredAttribution(new Candidate('BTC', BTC, ['m-market']), 60, [
                new AppliedRule('SALE_OR_SETTLEMENT', 60),
                new AppliedRule('CLASSIFICATION_UNAVAILABLE', 0)
            ], BTC_JUSTIFICATION, ClassificationStatus.UNAVAILABLE),
            new ScoredAttribution(new Candidate('ETH', ETH, ['m-forum', 'm-market']), 75, [
                new AppliedRule('SALE_OR_SETTLEMENT', 60),
                new AppliedRule('INDEPENDENT_SOURCES', 15)
            ], ETH_JUSTIFICATION, ClassificationStatus.NOT_EXCLUDED),
        ], 'confidence-1'))
        id
    }

    private Map json(String uri) {
        new JsonSlurper().parseText(client.get().uri(uri).exchange().expectStatus().isOk().expectBody(String).returnResult().responseBody) as Map
    }

    private List jsonList(String uri) {
        new JsonSlurper().parseText(client.get().uri(uri).exchange().expectStatus().isOk().expectBody(String).returnResult().responseBody) as List
    }

    def "the report lists Attributions by Confidence Level, each with its Justification, Mentions and scoring rules"() {
        given:
        def id = partial()

        when:
        def report = json("/api/1/analyses/${id}/report")

        then:
        report.analysis.id == id
        report.analysis.status == 'PARTIAL'
        report.analysis.algorithmVersion == 'confidence-1'
        report.attributions*.confidenceLevel == [75, 60]
        report.attributions*.address == [ETH, BTC]
        report.attributions*.network == ['ETH', 'BTC']
        report.attributions*.justification == [
            ETH_JUSTIFICATION,
            BTC_JUSTIFICATION
        ]
        report.attributions[0].mentions*.id == ['m-forum', 'm-market']
        report.attributions[0].mentions[0].url == 'https://forum.example/t/1'
        report.attributions[0].mentions[0].collectedAt == '2026-11-21T10:00:00Z'
        report.attributions[0].mentions[0].context.contains('zenek_fx')
        report.attributions[0].scoringRules == [
            [rule: 'SALE_OR_SETTLEMENT', contribution: 60],
            [rule: 'INDEPENDENT_SOURCES', contribution: 15]
        ]
        report.attributions*.addressActivity == [null, null]
    }

    def "an Attribution whose classification was unavailable is flagged, not shown as confirmed private"() {
        when:
        def report = json("/api/1/analyses/${partial()}/report")

        then:
        report.attributions.find {
            it.address == BTC
        }.classificationStatus == 'UNAVAILABLE'
        report.attributions.find {
            it.address == ETH
        }.classificationStatus == 'NOT_EXCLUDED'
        report.limitations == [
            'CLASSIFICATION_UNAVAILABLE',
            'ANALYSIS_INCOMPLETE'
        ]
    }

    def "Excluded Addresses are separate from Attributions and keep their reason, classification source and Mentions"() {
        when:
        def report = json("/api/1/analyses/${partial()}/report")

        then:
        !(KRAKEN in report.attributions*.address)
        report.excludedAddresses.size() == 1
        report.excludedAddresses[0].address == KRAKEN
        report.excludedAddresses[0].network == 'ETH'
        report.excludedAddresses[0].reason == 'EXCHANGE'
        report.excludedAddresses[0].classificationSource == 'Own label list: Kraken'
        report.excludedAddresses[0].mentions*.url == ['https://p2p.example/ad/2']
    }

    def "the Mentions keep source, URL, collection time and context, and every detected address is validated"() {
        when:
        def mentions = jsonList("/api/1/analyses/${partial()}/mentions")

        then:
        mentions*.id == ['m-forum', 'm-market']
        mentions[0].sourceName == 'synthetic'
        mentions[0].url == 'https://forum.example/t/1'
        mentions[0].collectedAt == '2026-11-21T10:00:00Z'
        mentions[0].detectedAddresses == [
            [address: ETH_EIP55, network: 'ETH', valid: true, validationFailure: null],
            [address: ETH_BAD_CHECKSUM, network: null, valid: false, validationFailure: 'CHECKSUM_MISMATCH'],
        ]
        mentions[1].detectedAddresses*.network == ['ETH', 'BTC']
    }

    def "a failed Analysis is reported as failed with its limitations, never as an Analysis with zero findings"() {
        given:
        def id = begin()
        recorder.finish(id, new AnalysisResult(AnalysisStatus.FAILED, [
            new StageFailure(AnalysisStage.SEARCH, 'ALL_SOURCES_UNAVAILABLE')
        ], [
            ReportLimitation.SOURCE_UNAVAILABLE,
            ReportLimitation.ANALYSIS_INCOMPLETE
        ], [
            new SourceReport('synthetic', SourceStatus.UNAVAILABLE, true, null)
        ], [], [], [], [], [], null))

        when:
        def report = json("/api/1/analyses/${id}/report")

        then:
        report.analysis.status == 'FAILED'
        report.analysis.sources*.status == ['UNAVAILABLE']
        report.attributions.isEmpty()
        report.limitations == [
            'SOURCE_UNAVAILABLE',
            'ANALYSIS_INCOMPLETE'
        ]
        jsonList("/api/1/analyses/${id}/mentions").isEmpty()
    }

    def "a completed Analysis without Mentions gives an empty report without limitations"() {
        given:
        def id = begin()
        recorder.finish(id, new AnalysisResult(AnalysisStatus.COMPLETED, [], [], [
            new SourceReport('synthetic', SourceStatus.CHECKED, true, AT)
        ], [], [], [], [], [], null))

        when:
        def report = json("/api/1/analyses/${id}/report")

        then:
        report.analysis.status == 'COMPLETED'
        report.attributions.isEmpty()
        report.excludedAddresses.isEmpty()
        report.limitations.isEmpty()
    }

    def "reading the report or Mentions of a running Analysis is refused, so it is not mistaken for no results"() {
        given:
        def id = begin()

        expect:
        ['report', 'mentions'].each {
            client.get().uri("/api/1/analyses/${id}/${it}").exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody().jsonPath('$.message').isEqualTo('Analiza jeszcze trwa. Wynik będzie dostępny po jej zakończeniu.')
        }
        repository.findById(id).get().status == AnalysisStatus.RUNNING
    }

    def "an unknown Analysis is a 404 for the report and the Mentions"() {
        expect:
        ['report', 'mentions'].each {
            client.get().uri("/api/1/analyses/missing-${UUID.randomUUID()}/${it}").exchange()
                    .expectStatus().isNotFound()
                    .expectBody().jsonPath('$.message').isEqualTo('Nie znaleziono analizy o podanym identyfikatorze.')
        }
    }

    def "reopening the report returns the same saved result and never changes the Analysis"() {
        given:
        def id = partial()
        def stored = repository.findById(id).get()

        when:
        def first = json("/api/1/analyses/${id}/report")
        def second = json("/api/1/analyses/${id}/report")

        then:
        first == second
        repository.findById(id).get() == stored
    }
}
