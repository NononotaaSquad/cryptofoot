package squad.nononotaa.backend.analysis

import java.time.Instant

interface MentionSource {
    val sourceName: String

    val synthetic: Boolean

    fun findMentions(identifiers: List<CaseIdentifier>): List<RawMention>
}

data class RawMention(
    val url: String,
    val collectedAt: Instant,
    val content: String,
)
