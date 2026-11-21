package squad.nononotaa.backend.analysis.synthetic

data class SyntheticFixture(
    val version: String,
    val synthetic: Boolean,
    val person: FixturePerson,
    val mentions: List<FixtureMention>,
)

data class FixturePerson(
    val firstName: String,
    val lastName: String,
    val identifiers: List<FixtureIdentifier>,
)

data class FixtureIdentifier(
    val type: String,
    val value: String,
)

data class FixtureMention(
    val url: String,
    val collectedAt: String,
    val content: String,
)
