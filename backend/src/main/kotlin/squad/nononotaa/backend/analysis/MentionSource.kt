package squad.nononotaa.backend.analysis

import java.time.Instant

interface MentionSource {
    val sourceName: String

    val synthetic: Boolean

    fun findMentions(identifiers: List<CaseIdentifier>): List<RawMention>

    // earlierAnalyses: how many Analyses of the same Case started before this one (0 for the first). A real source
    // answers with the web as it is now; only the synthetic demo source plays a web that changes between runs (S11)
    fun findMentions(
        identifiers: List<CaseIdentifier>,
        earlierAnalyses: Int,
    ): List<RawMention> = findMentions(identifiers)
}

data class RawMention @JvmOverloads constructor(
    val url: String,
    val collectedAt: Instant,
    val content: String,
    val author: String? = null,
)

class SourceNotCheckedException(message: String) : RuntimeException(message)

class SourceUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
