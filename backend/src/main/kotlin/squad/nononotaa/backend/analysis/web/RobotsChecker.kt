package squad.nononotaa.backend.analysis.web

import org.springframework.stereotype.Component
import squad.nononotaa.backend.util.logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RobotsChecker(
    private val userAgent: String = "OLCYS-Bot/1.0",
) {
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val cache = ConcurrentHashMap<String, ParsedRobots>()

    fun canFetch(url: String): Boolean {
        val uri = URI.create(url)
        val base = "${uri.scheme}://${uri.host}" + (if (uri.port > 0) ":${uri.port}" else "")
        val path = uri.path.ifEmpty { "/" }
        val robots = cache.computeIfAbsent(base) { fetchRobots(it) }
        val allowed = robots.isAllowed(path, userAgent)
        if (!allowed) {
            logger.debug("robots.txt blocks base={} path={} agent={}", base, path, userAgent)
        }
        return allowed
    }

    private fun fetchRobots(base: String): ParsedRobots {
        val robotsUrl = "$base/robots.txt"
        return try {
            val request = HttpRequest.newBuilder(URI.create(robotsUrl)).timeout(Duration.ofSeconds(5)).GET().build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                ParsedRobots.parse(response.body())
            } else {
                logger.debug("robots.txt not found status={} base={}", response.statusCode(), base)
                ParsedRobots.allowAll()
            }
        } catch (e: Exception) {
            logger.debug("robots.txt fetch failed base={} error={}", base, e.message)
            ParsedRobots.allowAll()
        }
    }

    companion object {
        private val logger = logger()
    }
}

data class ParsedRobots(
    val groups: List<RobotsGroup>,
) {
    fun isAllowed(
        path: String,
        userAgent: String,
    ): Boolean {
        val agentKey = userAgent.removeSuffix("/*").substringBefore('/').lowercase()
        val matching =
            groups
                .filter { group ->
                    group.agents.any { agent ->
                        agent == "*" || agentKey.contains(agent, ignoreCase = true)
                    }
                }.minByOrNull { group -> if (group.agents.contains("*")) 1 else 0 }
        val rules = matching?.rules ?: return true
        var bestMatch: Pair<Int, Boolean>? = null
        for (rule in rules) {
            if (path.startsWith(rule.path)) {
                val current = rule.path.length to !rule.disallow
                if (bestMatch == null || current.first > bestMatch.first) {
                    bestMatch = current
                }
            }
        }
        return bestMatch?.second ?: true
    }

    companion object {
        fun allowAll() = ParsedRobots(emptyList())

        fun parse(content: String): ParsedRobots {
            val groups = mutableListOf<RobotsGroup>()
            var currentAgents = mutableListOf<String>()
            var currentRules = mutableListOf<RobotsRule>()
            for (rawLine in content.lines()) {
                val line = rawLine.trim().substringBefore('#').trim()
                if (line.isEmpty()) continue
                val colon = line.indexOf(':')
                if (colon < 0) continue
                val field = line.substring(0, colon).trim().lowercase()
                val value = line.substring(colon + 1).trim()
                when (field) {
                    "user-agent" -> {
                        if (currentRules.isNotEmpty()) {
                            groups += RobotsGroup(currentAgents.toList(), currentRules.toList())
                            currentAgents = mutableListOf()
                            currentRules = mutableListOf()
                        }
                        currentAgents += value.lowercase()
                    }
                    "disallow" -> if (value.isNotEmpty()) currentRules += RobotsRule(value, disallow = true)
                    "allow" -> if (value.isNotEmpty()) currentRules += RobotsRule(value, disallow = false)
                }
            }
            if (currentAgents.isNotEmpty() || currentRules.isNotEmpty()) {
                groups += RobotsGroup(currentAgents.toList(), currentRules.toList())
            }
            return ParsedRobots(groups)
        }
    }
}

data class RobotsGroup(
    val agents: List<String>,
    val rules: List<RobotsRule>,
)

data class RobotsRule(
    val path: String,
    val disallow: Boolean,
)
