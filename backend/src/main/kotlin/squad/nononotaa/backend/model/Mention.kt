package squad.nononotaa.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import squad.nononotaa.backend.analysis.FoundMention
import java.security.MessageDigest
import java.time.Instant

@Document("mentions")
data class Mention @JvmOverloads constructor(
    @Id val id: String? = null,
    val sourceName: String,
    val url: String,
    val collectedAt: Instant,
    val context: String?,
    val detectedAddresses: List<String> = emptyList(),
    @Indexed val identity: String,
    val author: String? = null,
) {
    fun toFoundMention(): FoundMention = FoundMention(checkNotNull(id), sourceName, url, collectedAt, context, detectedAddresses, author)

    companion object {
        @JvmStatic
        fun computeIdentity(
            sourceName: String,
            url: String,
            context: String?,
        ): String {
            val input = "$sourceName\n$url\n${context ?: ""}"
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        @JvmStatic
        @JvmOverloads
        fun create(
            sourceName: String,
            url: String,
            collectedAt: Instant,
            context: String?,
            detectedAddresses: List<String> = emptyList(),
            author: String? = null,
        ): Mention = Mention(
            sourceName = sourceName,
            url = url,
            collectedAt = collectedAt,
            context = context,
            detectedAddresses = detectedAddresses,
            identity = computeIdentity(sourceName, url, context),
            author = author,
        )
    }
}
