package squad.nononotaa.backend.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class TestRequest(
    @field:NotBlank val name: String,
)

// TODO E1.1/E1.3: class-level constraint "at least one narrowing identifier" (fullName does not count), Polish message naming the fields
data class CreateCaseRequest(
    val nickname: String? = null,
    val login: String? = null,
    @field:Email val email: String? = null,
    val messengerId: String? = null,
    val profileUrl: String? = null,
    val fullName: String? = null,
)

// TODO E6.4: add cadence/timezone once E6-D3 is decided
data class UpdateScheduleRequest(
    val enabled: Boolean,
)
