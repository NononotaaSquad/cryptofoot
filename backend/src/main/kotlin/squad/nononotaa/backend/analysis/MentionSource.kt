package squad.nononotaa.backend.analysis

import java.time.Instant

interface MentionSource {
    val sourceName: String

    val synthetic: Boolean

    fun findMentions(identifiers: List<CaseIdentifier>): List<RawMention>
}

data class RawMention @JvmOverloads constructor(
    val url: String,
    val collectedAt: Instant,
    val content: String,
    val author: String? = null,
)

class SourceNotCheckedException(message: String) : RuntimeException(message)

class SourceUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
