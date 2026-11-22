package squad.nononotaa.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import squad.nononotaa.backend.analysis.AddressWindow
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
    // Mentions stored before algorithm confidence-2 have no windows
    val addressWindows: List<AddressWindow> = emptyList(),
) {
    fun toFoundMention(): FoundMention = FoundMention(checkNotNull(id), sourceName, url, collectedAt, context, detectedAddresses, author, addressWindows)

    companion object {
        // Bump when the hashed input changes: stored Mentions without address windows must not be reused
        private const val IDENTITY_VERSION = "mention-identity-2"

        // the full content is hashed but never stored (P3)
        @JvmStatic
        fun computeIdentity(
            sourceName: String,
            url: String,
            content: String,
        ): String {
            val input = "$IDENTITY_VERSION\n$sourceName\n$url\n$content"
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        @JvmStatic
        @JvmOverloads
        fun create(
            sourceName: String,
            url: String,
            collectedAt: Instant,
            content: String,
            context: String?,
            detectedAddresses: List<String> = emptyList(),
            author: String? = null,
            addressWindows: List<AddressWindow> = emptyList(),
        ): Mention = Mention(
            sourceName = sourceName,
            url = url,
            collectedAt = collectedAt,
            context = context,
            detectedAddresses = detectedAddresses,
            identity = computeIdentity(sourceName, url, content),
            author = author,
            addressWindows = addressWindows,
        )
    }
}
