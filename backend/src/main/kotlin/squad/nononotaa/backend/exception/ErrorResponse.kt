package squad.nononotaa.backend.exception

import org.springframework.http.HttpStatus

data class ErrorResponse(
    val code: Int,
    val status: HttpStatus,
    val message: String,
    val errors: List<FieldErrorResponse> = emptyList(),
) {
    constructor(status: HttpStatus, throwable: Throwable) : this(status, throwable.message ?: throwable.toString())

    @JvmOverloads
    constructor(
        status: HttpStatus,
        message: String,
        errors: List<FieldErrorResponse> = emptyList(),
    ) : this(status.value(), status, message, errors)
}

data class FieldErrorResponse(
    val field: String?,
    val message: String,
)
