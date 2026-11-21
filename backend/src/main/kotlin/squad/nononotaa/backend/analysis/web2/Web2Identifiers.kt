package squad.nononotaa.backend.analysis.web2

import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.IdentifierType
import java.net.URI

internal object Web2Identifiers {
    // a login or nickname of the Case, or the first path segment of a profile URL of one of the hosts
    fun handles(
        identifiers: List<CaseIdentifier>,
        hosts: Set<String>,
        profilePath: (List<String>) -> String?,
        valid: Regex,
        limit: Int,
    ): List<String> = identifiers
        .mapNotNull {
            when (it.type) {
                IdentifierType.LOGIN, IdentifierType.NICKNAME -> it.value.trim().removePrefix("@")
                IdentifierType.PROFILE_URL -> pathSegments(it.value, hosts)?.let(profilePath)
                else -> null
            }
        }.filter { valid.matches(it) }
        .distinctBy { it.lowercase() }
        .take(limit)

    fun pathSegments(
        value: String,
        hosts: Set<String>,
    ): List<String>? {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return null
        if (uri.scheme !in setOf("http", "https") || uri.host?.lowercase() !in hosts) return null
        return uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
    }
}
