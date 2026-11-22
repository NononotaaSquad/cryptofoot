package squad.nononotaa.backend.analysis.synthetic

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.MentionSource
import squad.nononotaa.backend.analysis.RawMention

@Component
// needs both switches: production keeps the demo data but not the always-unavailable source (ADR 0003)
@ConditionalOnProperty(name = ["sources.synthetic.enabled", "sources.synthetic-broken.enabled"], havingValue = "true", matchIfMissing = true)
class SyntheticFailingMentionSource : MentionSource {
    override val sourceName = "synthetic-broken"

    override val synthetic = true

    override fun findMentions(identifiers: List<CaseIdentifier>): List<RawMention> = throw IllegalStateException("Synthetic source simulating a failure (demo partial analysis)")
}
