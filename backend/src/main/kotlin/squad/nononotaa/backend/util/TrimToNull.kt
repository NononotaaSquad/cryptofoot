package squad.nononotaa.backend.util

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

class TrimToNull : ValueDeserializer<String?>() {
    override fun deserialize(
        parser: JsonParser,
        context: DeserializationContext,
    ): String? = parser.valueAsString?.trim()?.takeIf { it.isNotEmpty() }
}
