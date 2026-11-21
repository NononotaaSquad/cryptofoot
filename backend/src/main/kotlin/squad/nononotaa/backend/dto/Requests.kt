package squad.nononotaa.backend.dto

import jakarta.validation.constraints.NotBlank

data class TestRequest(
    @field:NotBlank val name: String,
)
