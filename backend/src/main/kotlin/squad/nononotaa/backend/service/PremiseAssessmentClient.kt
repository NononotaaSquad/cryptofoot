package squad.nononotaa.backend.service

import org.springframework.ai.decision.DecisionModel
import org.springframework.ai.decision.DecisionPrompt
import org.springframework.ai.decision.DecisionResponse
import org.springframework.ai.decision.input.DecisionInput
import org.springframework.ai.decision.question.ChoiceQuestion
import org.springframework.ai.decision.question.DecisionQuestion
import org.springframework.ai.decision.question.YesNoQuestion
import org.springframework.ai.systemone.api.exception.SystemOneApiException
import org.springframework.ai.systemone.api.exception.SystemOneApiTimeoutException
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import squad.nononotaa.backend.analysis.KindProbabilities
import squad.nononotaa.backend.analysis.PremiseAnswers
import squad.nononotaa.backend.analysis.PremiseAssessment
import squad.nononotaa.backend.analysis.PremiseAssessments
import squad.nononotaa.backend.analysis.PremiseToAssess
import squad.nononotaa.backend.analysis.YesNoProbabilities
import squad.nononotaa.backend.model.AssessmentMode
import squad.nononotaa.backend.model.AuthorMatch
import squad.nononotaa.backend.util.logger
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** The model is off (the default, or on without a key): no call, no answer, the Analysis runs on rules (D13, P20). */
class RulesOnlyPremiseAssessment : PremiseAssessment {
    override val mode = AssessmentMode.RULES_ONLY

    override fun assess(
        premises: List<PremiseToAssess>,
        identifiers: List<CaseIdentifier>,
    ) = PremiseAssessments()
}

/**
 * Asks the decision model "what does this text say about the address?" (D4, D6): one request per Premise, in
 * parallel on virtual threads, [timeout] per request, no retry and no cache (D7, P18). A Premise whose request fails
 * in any way is left out of the answers, so it falls back to the rules (D13). Logs never hold the text window, the
 * state, a body, an exception message or the key, only counts, durations, status codes and request ids.
 */
class ModelPremiseAssessment(
    private val model: DecisionModel,
    private val configuredModel: String,
    private val timeout: Duration,
) : PremiseAssessment {
    override val mode = AssessmentMode.WITH_MODEL

    override fun assess(
        premises: List<PremiseToAssess>,
        identifiers: List<CaseIdentifier>,
    ): PremiseAssessments {
        if (premises.isEmpty()) return PremiseAssessments(modelVersion = configuredModel)
        val started = System.nanoTime()
        val deadline = started + timeout.toNanos()
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        val answers = LinkedHashMap<String, PremiseAnswers>()
        var echoedVersion: String? = null
        var failures = 0
        try {
            val calls = premises.map { premise -> premise.premiseId to executor.submit<Answered> { ask(premise, identifiers) } }
            for ((index, call) in calls.withIndex()) {
                val (premiseId, future) = call
                val answered =
                    try {
                        future.get(maxOf(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        calls.drop(index).forEach { it.second.cancel(true) }
                        failures += calls.size - index
                        break
                    } catch (e: TimeoutException) {
                        future.cancel(true)
                        warnNoAnswer("timeout", null, null)
                        null
                    } catch (e: ExecutionException) {
                        warnNoAnswer(e.cause ?: e)
                        null
                    }
                if (answered == null) {
                    failures++
                } else {
                    answers[premiseId] = answered.answers
                    if (echoedVersion == null) echoedVersion = answered.modelVersion
                }
            }
        } finally {
            executor.shutdownNow()
        }
        logger.info(
            "Decision model assessed {} of {} Premises ({} without answer) in {} ms",
            answers.size,
            premises.size,
            failures,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
        )
        return PremiseAssessments(answers, echoedVersion ?: configuredModel)
    }

    private fun ask(
        premise: PremiseToAssess,
        identifiers: List<CaseIdentifier>,
    ): Answered {
        val prompt =
            DecisionPrompt
                .builder()
                .input(DecisionInput.fields(premiseState(premise, identifiers)))
                .questions(PREMISE_QUESTIONS)
                .build()
        return readAnswers(model.call(prompt))
    }

    private fun warnNoAnswer(failure: Throwable) {
        when (failure) {
            is SystemOneApiTimeoutException -> warnNoAnswer("timeout", null, null)

            // status and request id only: the message of SystemOneApiException quotes the error from the response body
            is SystemOneApiException -> warnNoAnswer(failure.javaClass.simpleName, failure.status(), failure.requestId())

            is UnreadableAnswerException -> warnNoAnswer("unreadable answer", null, failure.requestId)

            // the message of any other exception may quote a response body: the class name only
            else -> warnNoAnswer(failure.javaClass.simpleName, null, null)
        }
    }

    private fun warnNoAnswer(
        reason: String,
        status: Int?,
        requestId: String?,
    ) {
        logger.warn("Decision model gave no answer for a Premise: reason={}, status={}, requestId={}", reason, status, requestId)
    }

    companion object {
        private val logger = logger()
    }
}

internal data class Answered(
    val answers: PremiseAnswers,
    val modelVersion: String?,
)

/** The answer came back but cannot be read as the three answers; never carries any answer content. */
internal class UnreadableAnswerException(
    val requestId: String?,
) : RuntimeException("unreadable decision model answer")

// Question set `premise-questions-1` (P21): any change of the wording below is a new QUESTION_SET_VERSION.
const val QUESTION_KIND = "rodzaj_wpisu"
const val QUESTION_DECLARATION = "deklaracja"
const val QUESTION_WARNING = "ostrzezenie"
const val KIND_OWN_DECLARATION = "wlasna_deklaracja"
const val KIND_SALE_OR_SETTLEMENT = "sprzedaz_lub_rozliczenie"
const val KIND_DISCUSSION = "dyskusja"
const val KIND_MASS_COMMENT = "komentarz_masowy"

val PREMISE_QUESTIONS: Map<String, DecisionQuestion> =
    linkedMapOf(
        QUESTION_KIND to
            ChoiceQuestion
                .builder()
                .text("Jaki to rodzaj wpisu w odniesieniu do ocenianego adresu?")
                .option(KIND_OWN_DECLARATION, "Autor wpisu podaje oceniany adres jako swój, np. do wpłat, darowizn lub kontaktu.")
                .option(
                    KIND_SALE_OR_SETTLEMENT,
                    "Oceniany adres służy do zapłaty lub rozliczenia w ogłoszeniu sprzedaży, zamówieniu albo transakcji.",
                ).option(KIND_DISCUSSION, "Oceniany adres pojawia się w rozmowie, pytaniu, analizie lub relacji o adresie.")
                .option(KIND_MASS_COMMENT, "Wpis jest spamem, masową reklamą lub automatycznym komentarzem z adresem.")
                .build(),
        QUESTION_DECLARATION to
            YesNoQuestion
                .builder()
                .text("Czy autor tego tekstu przedstawia oceniany adres jako swój własny? Chodzi o autora tekstu, nie o inną osobę.")
                .whenYes("Autor tekstu pisze o ocenianym adresie jako o swoim, np. „mój adres”, „wpłaty na”.")
                .whenNo("Autor tekstu nie przypisuje sobie ocenianego adresu albo przypisuje go komuś innemu.")
                .build(),
        QUESTION_WARNING to
            YesNoQuestion
                .builder()
                .text("Czy tekst mówi czytelnikom, żeby nie używali ocenianego adresu albo mu nie ufali?")
                .whenYes("Tekst ostrzega przed ocenianym adresem, np. oszustwo, kradzież, adres nieaktualny, „nie wpłacajcie”.")
                .whenNo("Tekst nie ostrzega przed ocenianym adresem.")
                .build(),
    )

/**
 * The state of one request (P16): exactly these keys, in this order. Never the URL, the Case id, the identifiers as
 * fields or Analysis results; the window is masked (P15).
 */
fun premiseState(
    premise: PremiseToAssess,
    identifiers: List<CaseIdentifier>,
): Map<String, Any> = linkedMapOf(
    "tekst" to maskPremiseText(premise.text, premise.address, identifiers),
    "oceniany_adres" to premise.address,
    "autor_jest_badana_osoba" to
        when (premise.facts.authorIsPerson) {
            AuthorMatch.YES -> "tak"
            AuthorMatch.NO -> "nie"
            AuthorMatch.UNKNOWN -> "nieznany"
        },
    "liczba_adresow_we_wpisie" to premise.facts.addressCount,
    "serwis" to (premise.facts.host ?: "nieznany"),
)

private const val EMAIL_PLACEHOLDER = "[e-mail]"
private const val MESSENGER_PLACEHOLDER = "[komunikator]"
private const val FULL_NAME_PLACEHOLDER = "[imię i nazwisko]"

private val anyEmail = Regex("""[\p{L}\p{N}._%+\-]+@[\p{L}\p{N}\-]+(\.[\p{L}\p{N}\-]+)*\.\p{L}{2,}""")

/** A letter and its Polish diacritic forms match each other, so "Zolkiewski" masks "Żółkiewski" and back. */
private val polishLetterGroups = listOf("aą", "cć", "eę", "lł", "nń", "oó", "sś", "zźż")

/**
 * Masks the window before it leaves the process (P15): the Case's e-mail and every other e-mail address become
 * `[e-mail]`, the Case's messenger ID `[komunikator]`, the Case's full name `[imię i nazwisko]`. Case-insensitive;
 * identifier values are matched literally, never as a regex. The nickname and the login stay. The [address] is never
 * touched: the text is masked around each of its occurrences.
 */
fun maskPremiseText(
    text: String,
    address: String,
    identifiers: List<CaseIdentifier>,
): String {
    val masks = buildList {
        identifiers.valuesOf(IdentifierType.EMAIL).forEach { add(literalPattern(it) to EMAIL_PLACEHOLDER) }
        add(anyEmail to EMAIL_PLACEHOLDER)
        identifiers.valuesOf(IdentifierType.MESSENGER_ID).forEach { add(literalPattern(it) to MESSENGER_PLACEHOLDER) }
        identifiers.valuesOf(IdentifierType.FULL_NAME).forEach { name -> fullNamePatterns(name).forEach { add(it to FULL_NAME_PLACEHOLDER) } }
    }
    val mask = { part: String -> masks.fold(part) { masked, (pattern, placeholder) -> pattern.replace(masked, placeholder) } }
    if (address.isBlank()) return mask(text)
    // the address as written may differ in case from the canonical form (ETH checksum)
    val occurrences = Regex(Regex.escape(address), RegexOption.IGNORE_CASE).findAll(text).toList()
    val masked = StringBuilder()
    var from = 0
    for (occurrence in occurrences) {
        masked.append(mask(text.substring(from, occurrence.range.first))).append(occurrence.value)
        from = occurrence.range.last + 1
    }
    return masked.append(mask(text.substring(from))).toString()
}

private fun List<CaseIdentifier>.valuesOf(type: IdentifierType) = filter { it.type == type }.map { it.value.trim() }.filter { it.isNotEmpty() }

// Kotlin adds UNICODE_CASE to IGNORE_CASE on the JVM, so "Ż" matches "ż"
private val caseInsensitive = setOf(RegexOption.IGNORE_CASE)

/** The value as literal text, not inside a longer word (a boundary only where the value starts or ends with one). */
private fun literalPattern(value: String): Regex {
    val start = if (value.first().isLetterOrDigit()) "(?<![\\p{L}\\p{N}_])" else ""
    val end = if (value.last().isLetterOrDigit()) "(?![\\p{L}\\p{N}_])" else ""
    return Regex(start + Regex.escape(value) + end, caseInsensitive)
}

/**
 * "Jan Kowalski" also matches "Jana Kowalskiego" (Polish declension: letters may follow each part), "jan  kowalski",
 * "Kowalski Jan" and the forms with or without Polish diacritics.
 */
private fun fullNamePatterns(name: String): List<Regex> {
    val parts = name.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val orders = if (parts.size > 1) listOf(parts, parts.reversed()) else listOf(parts)
    return orders.map { order ->
        val body = order.joinToString("""\p{L}*\s+""") { part -> part.map(::letterPattern).joinToString("") }
        Regex("""(?<![\p{L}\p{N}])$body\p{L}*""", caseInsensitive)
    }
}

private fun letterPattern(char: Char): String {
    val group = polishLetterGroups.firstOrNull { char.lowercaseChar() in it }
    return if (group != null) "[$group]" else Regex.escape(char.toString())
}

/** Reads the three answers; [UnreadableAnswerException] when any is missing, of another type or not a probability. */
internal fun readAnswers(response: DecisionResponse): Answered {
    val requestId = runCatching { response.metadata.id }.getOrNull()?.takeIf { it.isNotBlank() }
    val answers =
        try {
            // a null answer or map throws NullPointerException here and is read as unreadable below
            val kind: Map<String, Double?> = response.getChoice(QUESTION_KIND).probabilities
            PremiseAnswers(
                kind =
                KindProbabilities(
                    ownDeclaration = probability(kind[KIND_OWN_DECLARATION], requestId),
                    saleOrSettlement = probability(kind[KIND_SALE_OR_SETTLEMENT], requestId),
                    discussion = probability(kind[KIND_DISCUSSION], requestId),
                    massComment = probability(kind[KIND_MASS_COMMENT], requestId),
                ),
                declaration = yesNo(response, QUESTION_DECLARATION, requestId),
                warning = yesNo(response, QUESTION_WARNING, requestId),
                requestId = requestId,
            )
        } catch (e: UnreadableAnswerException) {
            throw e
        } catch (e: RuntimeException) {
            // a missing answer or one of another type (e.g. a refusal); its message is not kept
            throw UnreadableAnswerException(requestId)
        }
    val version = runCatching { response.metadata.model }.getOrNull()?.takeIf { it.isNotBlank() }
    return Answered(answers, version)
}

private fun yesNo(
    response: DecisionResponse,
    question: String,
    requestId: String?,
): YesNoProbabilities {
    val yes = probability(response.getYesNo(question).probability, requestId)
    return YesNoProbabilities(yes = yes, no = 1.0 - yes)
}

private fun probability(
    value: Double?,
    requestId: String?,
): Double {
    if (value == null || value.isNaN() || value < 0.0 || value > 1.0) throw UnreadableAnswerException(requestId)
    return value
}
