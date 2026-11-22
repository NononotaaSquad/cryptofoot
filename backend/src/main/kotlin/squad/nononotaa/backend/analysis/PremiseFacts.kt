package squad.nononotaa.backend.analysis

import squad.nononotaa.backend.model.AuthorMatch
import java.net.URI
import java.text.Normalizer

fun premiseFacts(
    mention: FoundMention,
    window: AddressWindow,
    identifiers: List<CaseIdentifier>,
): PremiseFacts {
    val occurrences = occurrences(window.text, identifiers)
    return PremiseFacts(
        authorIsPerson = authorIsPerson(mention.author, identifiers),
        underProfile = underProfile(mention.url, identifiers),
        identifierTypes = occurrences.map { it.type }.distinct().sorted(),
        identifierDistance = distance(occurrences, window),
        conflictingEmail = conflictingEmail(window.text, identifiers),
        addressCount = mention.detectedAddresses.distinct().size,
        host = runCatching { URI(mention.url).host }.getOrNull()?.lowercase()?.removePrefix("www."),
        sourceName = mention.sourceName,
    )
}

// the wallet address is the Analyst's own entry, not a trace of the person in the text (D9-004)
private val TEXT_TYPES = IdentifierType.entries - IdentifierType.WALLET_ADDRESS
private val EMAIL = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
private val DIACRITICS = Regex("\\p{M}+")

// the window names someone else's e-mail and not the person's (D9-004)
private fun conflictingEmail(
    text: String,
    identifiers: List<CaseIdentifier>,
): Boolean {
    val own = identifiers.filter { it.type == IdentifierType.EMAIL }.map { it.value.trim().lowercase() }.filter { it.isNotEmpty() }
    if (own.isEmpty()) return false
    val found = EMAIL.findAll(text).map { it.value.lowercase() }.toList()
    return found.isNotEmpty() && found.none { it in own }
}

// the full name never links an address to the person (D9-004), so it has no distance
private val LINKING_TYPES = TEXT_TYPES - IdentifierType.FULL_NAME

/** Characters between the nearest linking identifier and the address, in the original window; 0 when they touch. */
private fun distance(
    occurrences: List<Occurrence>,
    window: AddressWindow,
): Int? {
    val addressEnd = window.addressStart + window.addressLength
    return occurrences
        .filter { it.type in LINKING_TYPES }
        .minOfOrNull { occurrence ->
            when {
                occurrence.range.last < window.addressStart -> window.addressStart - occurrence.range.last - 1
                occurrence.range.first >= addressEnd -> occurrence.range.first - addressEnd
                else -> 0
            }
        }
}

/** Where one identifier of the Case occurs in the window; [range] is in the original window text. */
private data class Occurrence(
    val type: IdentifierType,
    val range: IntRange,
)

private fun occurrences(
    text: String,
    identifiers: List<CaseIdentifier>,
): List<Occurrence> {
    val folded = FoldedText(text)
    val emails = EMAIL.findAll(text).toList()
    return identifiers.filter { it.type in TEXT_TYPES }.flatMap { identifier ->
        val value = identifier.value.trim()
        when {
            value.isEmpty() -> emptyList()

            // an e-mail is matched whole, so a longer address that contains it does not count
            identifier.type == IdentifierType.EMAIL ->
                emails.filter { it.value.equals(value, ignoreCase = true) }.map { Occurrence(identifier.type, it.range) }

            else -> {
                val needle = if (identifier.type == IdentifierType.PROFILE_URL) bareUrl(value) else value.removePrefix("@")
                folded.wholeWords(FoldedText(needle).text).map { Occurrence(identifier.type, it) }
            }
        }
    }
}

/**
 * The text folded like the keyword lists (lower case, no diacritics, `ł` as `l`, one space for a run of white space),
 * with the original index of every folded character, because folding can change the length.
 */
private class FoldedText(
    original: String,
) {
    val text: String
    private val origins: IntArray

    init {
        val folded = StringBuilder()
        val indices = ArrayList<Int>(original.length)
        original.forEachIndexed { index, char ->
            val piece =
                if (char.isWhitespace()) {
                    if (index > 0 && original[index - 1].isWhitespace()) "" else " "
                } else {
                    fold(char)
                }
            folded.append(piece)
            repeat(piece.length) { indices.add(index) }
        }
        text = folded.toString()
        origins = indices.toIntArray()
    }

    // no letter, digit or `_` directly before or after; a leading `@` in the text is not part of a word, so it is ignored
    fun wholeWords(needle: String): List<IntRange> {
        if (needle.isEmpty()) return emptyList()
        val found = mutableListOf<IntRange>()
        var at = text.indexOf(needle)
        while (at >= 0) {
            val end = at + needle.length
            if (!isWordChar(text.getOrNull(at - 1)) && !isWordChar(text.getOrNull(end))) found += origins[at]..origins[end - 1]
            at = text.indexOf(needle, at + 1)
        }
        return found
    }

    private fun isWordChar(char: Char?) = char != null && (char.isLetterOrDigit() || char == '_')

    private fun fold(char: Char) = Normalizer
        .normalize(char.lowercase().replace('ł', 'l'), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
}

private fun underProfile(
    url: String,
    identifiers: List<CaseIdentifier>,
): Boolean {
    val page = bareUrl(url)
    return identifiers.any { it.type == IdentifierType.PROFILE_URL && it.value.isNotBlank() && under(page, bareUrl(it.value)) }
}

private fun under(
    url: String,
    prefix: String,
) = url == prefix || listOf('/', '?', '#').any { url.startsWith("$prefix$it") }

private fun bareUrl(url: String) = url
    .trim()
    .lowercase()
    .removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .trimEnd('/')

// compared like the search compares provenance (P6); without a nickname or login there is nothing to compare (UNKNOWN)
private fun authorIsPerson(
    author: String?,
    identifiers: List<CaseIdentifier>,
): AuthorMatch {
    val name = author?.trim()?.removePrefix("@").orEmpty()
    val handles =
        identifiers
            .filter { it.type == IdentifierType.NICKNAME || it.type == IdentifierType.LOGIN }
            .map { it.value.trim().removePrefix("@") }
            .filter { it.isNotEmpty() }
    return when {
        name.isEmpty() || handles.isEmpty() -> AuthorMatch.UNKNOWN
        handles.any { it.equals(name, ignoreCase = true) } -> AuthorMatch.YES
        else -> AuthorMatch.NO
    }
}
