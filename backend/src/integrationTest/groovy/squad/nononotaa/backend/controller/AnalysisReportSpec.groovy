package squad.nononotaa.backend.controller

import groovy.json.JsonSlurper
import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import squad.nononotaa.backend.BaseSpec
import squad.nononotaa.backend.analysis.AddressActivity
import squad.nononotaa.backend.analysis.AddressActivityOutcome
import squad.nononotaa.backend.analysis.AddressWindow
import squad.nononotaa.backend.analysis.AnalysisRecorder
import squad.nononotaa.backend.analysis.AnalysisRequest
import squad.nononotaa.backend.analysis.AnalysisResult
import squad.nononotaa.backend.analysis.AnalysisStage
import squad.nononotaa.backend.analysis.AppliedRule
import squad.nononotaa.backend.analysis.Candidate
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.CashOutSignal
import squad.nononotaa.backend.analysis.ExcludedCandidate
import squad.nononotaa.backend.analysis.FoundMention
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.Premise
import squad.nononotaa.backend.analysis.PremiseFacts
import squad.nononotaa.backend.analysis.RejectedCandidate
import squad.nononotaa.backend.analysis.ScoredAttribution
import squad.nononotaa.backend.analysis.SourceReport
import squad.nononotaa.backend.analysis.StageFailure
import squad.nononotaa.backend.model.AddressActivityCoverageLimitation
import squad.nononotaa.backend.model.AddressActivityStatus
import squad.nononotaa.backend.model.AnalysisStatus
import squad.nononotaa.backend.model.AnalysisTrigger
import squad.nononotaa.backend.model.AssessedBy
import squad.nononotaa.backend.model.AuthorMatch
import squad.nononotaa.backend.model.CashOutSignalKind
import squad.nononotaa.backend.model.ClassificationStatus
import squad.nononotaa.backend.model.DismissalReason
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.PremiseKind
import squad.nononotaa.backend.model.PremiseStatus
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

    @Autowired
    MongoTemplate mongo

    String caseId = "case-${UUID.randomUUID()}"

    def forum = new FoundMention('m-forum', 'synthetic', 'https://forum.example/t/1', AT, 'zenek_fx: my ETH ' + ETH_EIP55, [ETH_EIP55, ETH_BAD_CHECKSUM])
    def market = new FoundMention('m-market', 'synthetic', 'https://p2p.example/ad/2', AT.plusSeconds(60), 'zenek_fx pays to ' + KRAKEN + ' and ' + BTC, [KRAKEN, BTC])
    def sale = new FoundMention('m-sale', 'synthetic', 'https://p2p.example/ad/3', AT.plusSeconds(120), 'zenek_fx sprzedam, płatność na ' + ETH_EIP55, [ETH_EIP55], 'zenek_fx')
    def saleFacts = new PremiseFacts(AuthorMatch.YES, false, [IdentifierType.NICKNAME], 0, false, 1, 'p2p.example', 'synthetic')
    def forumFacts = new PremiseFacts(AuthorMatch.UNKNOWN, true, [], null, true, 2, null, 'synthetic')

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
            [rule: 'SALE_OR_SETTLEMENT', contribution: 60, premiseIds: []],
            [rule: 'INDEPENDENT_SOURCES', contribution: 15, premiseIds: []]
        ]
        report.attributions*.addressActivity == [null, null]
        report.attributions*.addressActivityStatus == ['NOT_CHECKED', 'NOT_CHECKED']
    }

    def "stored Address Activity is reported with its totals, cash-out signals, provenance and limitations; missing data stays unavailable"() {
        given:
        def id = begin()
        def activity = new AddressActivity('mempool.space Bitcoin explorer API (mempool.space)', false, AT.plusSeconds(5), 'activity-1', new BigDecimal('2500'), 'BTC',
                new BigDecimal('1.23456789'), 0.5, 40, null, AT.minusSeconds(86400), [
                    new CashOutSignal(CashOutSignalKind.EXCHANGE, 't-exit', 'https://mempool.space/tx/t-exit', AT.minusSeconds(86400),
                    new BigDecimal('0.00012345'), 'BTC', '1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s', 'Own label list: Binance.com')
                ], [
                    AddressActivityCoverageLimitation.RECENT_TRANSACTIONS_ONLY,
                    AddressActivityCoverageLimitation.LABELLED_COUNTERPARTIES_ONLY
                ])
        def eth = new Candidate('ETH', ETH, ['m-forum'])
        recorder.finish(id, new AnalysisResult(AnalysisStatus.PARTIAL, [
            new StageFailure(AnalysisStage.ADDRESS_ACTIVITY, "ADDRESS_ACTIVITY_UNAVAILABLE:${ETH}".toString())
        ], [
            ReportLimitation.BLOCKCHAIN_DATA_UNAVAILABLE,
            ReportLimitation.ANALYSIS_INCOMPLETE
        ], [
            new SourceReport('synthetic', SourceStatus.CHECKED, true, AT)
        ], [forum, market], [], [], [], [
            new ScoredAttribution(new Candidate('BTC', BTC, ['m-market']), 60, [], '', ClassificationStatus.NOT_EXCLUDED,
            [], new AddressActivityOutcome(AddressActivityStatus.AVAILABLE, activity)),
            new ScoredAttribution(eth, 40, [], '', ClassificationStatus.NOT_EXCLUDED,
            [], new AddressActivityOutcome(AddressActivityStatus.UNAVAILABLE, null)),
            new ScoredAttribution(new Candidate('ETH', KRAKEN, ['m-market']), 0, [], '', ClassificationStatus.NOT_EXCLUDED, [], null),
        ], 'confidence-1'))

        when:
        def report = json("/api/1/analyses/${id}/report")

        then:
        report.attributions*.addressActivityStatus == [
            'AVAILABLE',
            'UNAVAILABLE',
            'NOT_CHECKED'
        ]
        report.attributions[0].addressActivity == [
            sourceName         : 'mempool.space Bitcoin explorer API (mempool.space)',
            synthetic          : false,
            checkedAt          : '2026-11-21T10:00:05Z',
            rulesVersion       : 'activity-1',
            stablecoinSwapThreshold: 2500,
            unit               : 'BTC',
            totalReceived      : 1.23456789,
            totalSent          : 0.5,
            transactionCount   : 40,
            firstActivityAt    : null,
            lastActivityAt     : '2026-11-20T10:00:00Z',
            cashOutSignals     : [
                [
                    kind               : 'EXCHANGE',
                    transactionId      : 't-exit',
                    transactionUrl     : 'https://mempool.space/tx/t-exit',
                    occurredAt         : '2026-11-20T10:00:00Z',
                    amount             : 0.00012345,
                    unit               : 'BTC',
                    counterpartyAddress: '1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s',
                    counterpartyLabel  : 'Own label list: Binance.com'
                ]
            ],
            coverageLimitations: [
                'RECENT_TRANSACTIONS_ONLY',
                'LABELLED_COUNTERPARTIES_ONLY'
            ]
        ]
        report.attributions[1].addressActivity == null
        report.attributions[2].addressActivity == null
        report.limitations == [
            'BLOCKCHAIN_DATA_UNAVAILABLE',
            'ANALYSIS_INCOMPLETE'
        ]
    }

    def "an Address Activity stored before the rules were versioned still reads back, with an unknown version"() {
        given:
        def id = begin()
        def activity = new AddressActivity('synthetic', true, AT, 'activity-1', new BigDecimal('1000'), 'BTC', 0.75, 0.25, 4,
                AT.minusSeconds(86400), AT, [], [])
        recorder.finish(id, new AnalysisResult(AnalysisStatus.COMPLETED, [], [], [
            new SourceReport('synthetic', SourceStatus.CHECKED, true, AT)
        ], [market], [], [], [], [
            new ScoredAttribution(new Candidate('BTC', BTC, ['m-market']), 60, [], '', ClassificationStatus.NOT_EXCLUDED,
            [], new AddressActivityOutcome(AddressActivityStatus.AVAILABLE, activity))
        ], 'confidence-1'))
        mongo.updateFirst(Query.query(Criteria.where('_id').is(id)), new Update()
                .unset('result.attributions.0.addressActivity.activity.rulesVersion')
                .unset('result.attributions.0.addressActivity.activity.stablecoinSwapThreshold'), 'analyses')

        when:
        def reported = json("/api/1/analyses/${id}/report").attributions[0].addressActivity

        then:
        reported.rulesVersion == null
        reported.stablecoinSwapThreshold == null
        reported.totalReceived == 0.75
        jsonList('/api/1/cases') != null
    }

    def "a confidence-1 Analysis stored before Premises reads back with no Premises and no Premise ids"() {
        given: 'the stored document has no Premise fields, as one written before confidence-2'
        def id = partial()
        mongo.updateFirst(Query.query(Criteria.where('_id').is(id)), new Update()
                .unset('result.attributions.$[].premises')
                .unset('result.attributions.$[].appliedRules.$[].premiseIds'), 'analyses')
        def stored = mongo.findById(id, Map, 'analyses')

        expect:
        stored.result.attributions.every {
            !it.containsKey('premises') && it.appliedRules.every { rule ->
                !rule.containsKey('premiseIds')
            }
        }

        when:
        def report = json("/api/1/analyses/${id}/report")

        then:
        report.attributions*.premises == [[], []]
        report.attributions*.scoringRules*.premiseIds.flatten().isEmpty()
        report.attributions*.scoringRules.flatten().size() == 4
    }

    private static Premise premise(FoundMention mention, PremiseStatus status, PremiseKind kind, int points, DismissalReason reason, PremiseFacts facts) {
        def text = mention.context
        def start = text.indexOf(ETH_EIP55)
        new Premise(Premise.idOf(mention.id, ETH), mention.id, ETH, new AddressWindow(ETH_EIP55, ETH, text, start, ETH_EIP55.length()),
                facts, status, kind, points, reason, AssessedBy.RULE)
    }

    private String withPremises(List<Premise> premises, List<AppliedRule> rules) {
        def id = begin()
        recorder.finish(id, new AnalysisResult(AnalysisStatus.COMPLETED, [], [], [
            new SourceReport('synthetic', SourceStatus.CHECKED, true, AT)
        ], [forum, sale], [], [], [], [
            new ScoredAttribution(new Candidate('ETH', ETH, ['m-forum', 'm-sale']), 60, rules, SALE, ClassificationStatus.NOT_EXCLUDED, premises)
        ], 'confidence-2'))
        id
    }

    def "each Attribution lists its Premises with the Mention, the window, the assessment and the facts, and each rule names its Premises"() {
        given:
        def counted = premise(sale, PremiseStatus.COUNTED, PremiseKind.SALE_OR_SETTLEMENT, 60, null, saleFacts)
        def dismissed = premise(forum, PremiseStatus.DISMISSED, null, 0, DismissalReason.CONFLICTING_EMAIL, forumFacts)
        def id = withPremises([counted, dismissed], [
            new AppliedRule('SALE_OR_SETTLEMENT', 60, [counted.id]),
            new AppliedRule('CONFLICTING_EMAIL', 0, [dismissed.id]),
        ])

        when:
        def attribution = json("/api/1/analyses/${id}/report").attributions[0]

        then:
        attribution.premises == [
            [id: 'm-sale:' + ETH, mentionId: 'm-sale', sourceName: 'synthetic', url: 'https://p2p.example/ad/3', author: 'zenek_fx',
                collectedAt: '2026-11-21T10:02:00Z', text: sale.context, addressStart: sale.context.indexOf(ETH_EIP55), addressLength: 42,
                status: 'COUNTED', kind: 'SALE_OR_SETTLEMENT', points: 60, dismissalReason: null, assessedBy: 'RULE',
                facts: [authorIsPerson: 'YES', underProfile: false, identifierTypes: ['NICKNAME'], identifierDistance: 0, conflictingEmail: false, addressCount: 1, host: 'p2p.example'],
                answers: null, undetermined: [], withdrawn: false],
            [id: 'm-forum:' + ETH, mentionId: 'm-forum', sourceName: 'synthetic', url: 'https://forum.example/t/1', author: null,
                collectedAt: '2026-11-21T10:00:00Z', text: forum.context, addressStart: forum.context.indexOf(ETH_EIP55), addressLength: 42,
                status: 'DISMISSED', kind: null, points: 0, dismissalReason: 'CONFLICTING_EMAIL', assessedBy: 'RULE',
                facts: [authorIsPerson: 'UNKNOWN', underProfile: true, identifierTypes: [], identifierDistance: null, conflictingEmail: true, addressCount: 2, host: null],
                answers: null, undetermined: [], withdrawn: false],
        ]
        attribution.scoringRules == [
            [rule: 'SALE_OR_SETTLEMENT', contribution: 60, premiseIds: ['m-sale:' + ETH]],
            [rule: 'CONFLICTING_EMAIL', contribution: 0, premiseIds: ['m-forum:' + ETH]],
        ]
        attribution.scoringRules*.premiseIds.flatten().every {
            it in attribution.premises*.id
        }
        attribution.mentions*.id == ['m-forum', 'm-sale']
    }

    def "a Premise whose Mention is missing from the snapshot is left out of the report rather than shown without its source"() {
        given:
        def orphan = new FoundMention('m-gone', 'synthetic', 'https://gone.example/1', AT, forum.context, [ETH_EIP55])
        def kept = premise(sale, PremiseStatus.COUNTED, PremiseKind.SALE_OR_SETTLEMENT, 60, null, saleFacts)
        def id = withPremises([
            kept,
            premise(orphan, PremiseStatus.COUNTED, PremiseKind.DISCUSSION, 40, null, forumFacts)
        ], [
            new AppliedRule('SALE_OR_SETTLEMENT', 60, [kept.id])
        ])

        when:
        def attribution = json("/api/1/analyses/${id}/report").attributions[0]

        then:
        attribution.premises*.id == ['m-sale:' + ETH]
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
