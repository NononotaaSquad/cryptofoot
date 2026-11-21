package squad.nononotaa.backend.analysis.synthetic

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.MentionSource
import squad.nononotaa.backend.analysis.RawMention
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Component
@ConditionalOnProperty(name = ["sources.synthetic-v2.enabled"], havingValue = "true", matchIfMissing = true)
class SyntheticMentionSourceV2(
    objectMapper: ObjectMapper,
) : MentionSource {
    private val fixture: SyntheticFixture = loadFixture(objectMapper)

    override val sourceName = "synthetic-github-v2"

    override val synthetic = true

    override fun findMentions(identifiers: List<CaseIdentifier>): List<RawMention> {
        val values = identifiers.map { it.value.trim().lowercase() }.toSet()
        return fixture.mentions
            .filter { mention ->
                values.any { v -> mention.content.lowercase().contains(v) }
            }
            .map { RawMention(it.url, Instant.parse(it.collectedAt), it.content) }
    }

    companion object {
        const val FIXTURE_RESOURCE = "fixtures/demo-github-v2.json"

        fun loadFixture(objectMapper: ObjectMapper): SyntheticFixture {
            val fixture = objectMapper.readValue(ClassPathResource(FIXTURE_RESOURCE).inputStream, SyntheticFixture::class.java)
            require(fixture.synthetic) { "$FIXTURE_RESOURCE must be marked synthetic" }
            return fixture
        }
    }
}
