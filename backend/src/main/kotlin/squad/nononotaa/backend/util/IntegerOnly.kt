package squad.nononotaa.backend.util

import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

// Jackson would silently truncate 1.5 to 1 for a Long field
class IntegerOnly : ValueDeserializer<Long?>() {
    override fun deserialize(
        parser: JsonParser,
        context: DeserializationContext,
    ): Long? = if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
        parser.longValue
    } else {
        context.handleUnexpectedToken(Long::class.java, parser) as Long?
    }
}
