package squad.nononotaa.backend.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.util.NarrowingIdentifier
import squad.nononotaa.backend.util.TrimToNull
import tools.jackson.databind.annotation.JsonDeserialize

data class TestRequest(
    @field:NotBlank val name: String,
)

@NarrowingIdentifier
data class CreateCaseRequest(
    @param:JsonDeserialize(using = TrimToNull::class) val nickname: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) val login: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) @field:Email val email: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) val messengerId: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) val profileUrl: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) val fullName: String? = null,
)

data class UpdateScheduleRequest(
    val enabled: Boolean,
    val cadence: SchedulePreset? = null,
)
