package squad.nononotaa.backend.service

import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import org.springframework.ai.decision.DecisionModel
import org.springframework.ai.decision.DecisionPrompt
import org.springframework.ai.decision.DecisionResponse
import org.springframework.ai.decision.DecisionResponseMetadata
import org.springframework.ai.decision.answer.ChoiceAnswer
import org.springframework.ai.decision.answer.YesNoAnswer
import org.springframework.ai.decision.input.FieldsInput
import spock.lang.Specification
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.PremiseFacts
import squad.nononotaa.backend.analysis.PremiseToAssess
import squad.nononotaa.backend.model.AssessmentMode
import squad.nononotaa.backend.model.AuthorMatch

class PremiseAssessmentClientSpec extends Specification {

    static final String ADDRESS = 'bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq'
    static final String OTHER_ADDRESS = '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed'

    static final List<CaseIdentifier> IDENTIFIERS = [
        new CaseIdentifier(IdentifierType.FULL_NAME, 'Paweł Żółkiewski'),
        new CaseIdentifier(IdentifierType.EMAIL, 'pawel.z@example.pl'),
        new CaseIdentifier(IdentifierType.MESSENGER_ID, 'tg:+48(600)123.456'),
        new CaseIdentifier(IdentifierType.NICKNAME, 'cryptopawel'),
        new CaseIdentifier(IdentifierType.LOGIN, 'pzolk'),
    ]

    def "masks the full name, the e-mails and the messenger ID, but keeps the address, the nickname and the login"() {
        given:
        def text = "Jestem cryptopawel (login pzolk), czyli PAWEŁ ŻÓŁKIEWSKI; pisz do pawel zolkiewskiego, " +
                "Pawel.Z@Example.pl albo tg:+48(600)123.456. Kopia dla anna.nowak@poczta.com. Wpłaty: $ADDRESS"

        when:
        def masked = PremiseAssessmentClientKt.maskPremiseText(text, ADDRESS, IDENTIFIERS)

        then:
        [
            'Paweł',
            'Żółkiewski',
            'pawel zol',
            'Zolkiewski',
            'pawel.z',
            'anna.nowak',
            '600'
        ].every {
            !masked.toLowerCase().contains(it.toLowerCase())
        }
        masked.count('[imię i nazwisko]') == 2
        masked.count('[e-mail]') == 2
        masked.count('[komunikator]') == 1
        masked.contains(ADDRESS)
        masked.contains('cryptopawel')
        masked.contains('pzolk')
    }

    def "the state has exactly the five keys in order and no URL or Case id"() {
        given:
        def premise = premise('mention-42:' + ADDRESS, ADDRESS, "Mój adres $ADDRESS, pisz pawel.z@example.pl")

        when:
        def state = PremiseAssessmentClientKt.premiseState(premise, IDENTIFIERS)

        then:
        state.keySet() as List == [
            'tekst',
            'oceniany_adres',
            'autor_jest_badana_osoba',
            'liczba_adresow_we_wpisie',
            'serwis'
        ]
        state.tekst == "Mój adres $ADDRESS, pisz [e-mail]".toString()
        state.oceniany_adres == ADDRESS
        state.autor_jest_badana_osoba == 'tak'
        state.liczba_adresow_we_wpisie == 2
        state.serwis == 'forum.example.org'
        !state.values().any {
            it.toString().contains('://') || it.toString().contains('mention-42')
        }
    }

    def "asks once per Premise and leaves out a Premise whose call fails"() {
        given:
        def model = new FakeDecisionModel(failFor: OTHER_ADDRESS)
        def adapter = new ModelPremiseAssessment(model, 'jev-latest', Duration.ofSeconds(5))
        def premises = [
            premise('m1:' + ADDRESS, ADDRESS, "Mój adres $ADDRESS"),
            premise('m1:' + OTHER_ADDRESS, OTHER_ADDRESS, "Nie wpłacajcie na $OTHER_ADDRESS"),
        ]

        when:
        def result = adapter.assess(premises, IDENTIFIERS)

        then:
        adapter.mode == AssessmentMode.WITH_MODEL
        model.prompts.size() == 2
        model.prompts.every {
            it.questions.keySet() as List == [
                'rodzaj_wpisu',
                'deklaracja',
                'ostrzezenie'
            ]
        }
        result.answers.keySet() == ['m1:' + ADDRESS] as Set
        result.modelVersion == 'jev-2026-11-01'

        and:
        def answer = result.answers['m1:' + ADDRESS]
        answer.kind.ownDeclaration == 0.7d
        answer.kind.saleOrSettlement == 0.1d
        answer.kind.discussion == 0.15d
        answer.kind.massComment == 0.05d
        answer.declaration.yes == 0.9d
        Math.abs(answer.declaration.no - 0.1d) < 1e-9
        answer.warning.yes == 0.2d
        answer.requestId == 'req-' + ADDRESS
    }

    def "the rules-only adapter makes no call and reports RULES_ONLY"() {
        given:
        def adapter = new RulesOnlyPremiseAssessment()

        when:
        def result = adapter.assess([
            premise('m1:' + ADDRESS, ADDRESS, ADDRESS)
        ], IDENTIFIERS)

        then:
        adapter.mode == AssessmentMode.RULES_ONLY
        result.answers.isEmpty()
        result.modelVersion == null
    }

    private static PremiseToAssess premise(String id, String address, String text) {
        def facts = new PremiseFacts(AuthorMatch.YES, false, [IdentifierType.NICKNAME], 3, false, 2, 'forum.example.org', 'Forum')
        new PremiseToAssess(id, address, text, facts)
    }

    static class FakeDecisionModel implements DecisionModel {
        String failFor
        List<DecisionPrompt> prompts = new CopyOnWriteArrayList<>()

        @Override
        DecisionResponse call(DecisionPrompt prompt) {
            prompts << prompt
            def address = ((FieldsInput) prompt.instructions).fields.oceniany_adres
            if (address == failFor) {
                throw new IllegalStateException('model down')
            }
            DecisionResponse.builder(prompt)
                    .answer('rodzaj_wpisu', ChoiceAnswer.builder()
                    .value('wlasna_deklaracja')
                    .probabilities([wlasna_deklaracja: 0.7d, sprzedaz_lub_rozliczenie: 0.1d, dyskusja: 0.15d, komentarz_masowy: 0.05d])
                    .confidence(0.7d)
                    .build())
                    .answer('deklaracja', YesNoAnswer.builder().probability(0.9d).build())
                    .answer('ostrzezenie', YesNoAnswer.builder().probability(0.2d).build())
                    .metadata(DecisionResponseMetadata.builder().id('req-' + address).model('jev-2026-11-01').build())
                    .build()
        }
    }
}
