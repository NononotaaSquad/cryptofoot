package squad.nononotaa.backend.exception

import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import squad.nononotaa.backend.util.logger
import tools.jackson.databind.DatabindException
import tools.jackson.databind.exc.InvalidNullException
import tools.jackson.module.kotlin.KotlinInvalidNullException

@RestControllerAdvice
class RestExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(ex: BadRequestException) = respond(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException) = respond(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(ex: ConflictException) = respond(HttpStatus.CONFLICT, ex)

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicateKeyException(ex: DuplicateKeyException): ResponseEntity<ErrorResponse> {
        // the driver message carries the duplicated value, which may be personal data
        log.warn("Write rejected by unique index exception={}", ex.javaClass.simpleName)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(HttpStatus.CONFLICT, DUPLICATE_MESSAGE))
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(ex: ValidationException) = respond(HttpStatus.UNPROCESSABLE_CONTENT, ex)

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", ex)
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        return ResponseEntity.status(status).body(ErrorResponse(status, status.reasonPhrase))
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = ex.bindingResult.allErrors.map { FieldErrorResponse((it as? FieldError)?.field, it.defaultMessage ?: INVALID_VALUE_MESSAGE) }
        return validationFailed(ex, errors, headers, request)
    }

    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors =
            ex.parameterValidationResults.flatMap { result ->
                result.resolvableErrors.map {
                    val field = (it as? FieldError)?.field ?: result.methodParameter.parameterName
                    FieldErrorResponse(field, it.defaultMessage ?: INVALID_VALUE_MESSAGE)
                }
            }
        return validationFailed(ex, errors, headers, request)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val cause = ex.mostSpecificCause as? DatabindException
        val field = cause?.let(::fieldPath) ?: return super.handleHttpMessageNotReadable(ex, headers, status, request)
        val message = if (cause is InvalidNullException) REQUIRED_MESSAGE else INVALID_VALUE_MESSAGE
        return validationFailed(ex, listOf(FieldErrorResponse(field, message)), headers, request)
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val status = HttpStatus.valueOf(statusCode.value())
        if (status.is5xxServerError) {
            log.error("Request failed", ex)
        } else if (body !is ErrorResponse) {
            log.warn("Request rejected status={} exception={}", status.value(), ex.javaClass.simpleName)
        }
        val errorBody = body as? ErrorResponse ?: ErrorResponse(status, clientMessage(status))
        return super.handleExceptionInternal(ex, errorBody, headers, statusCode, request)
    }

    private fun validationFailed(
        ex: Exception,
        errors: List<FieldErrorResponse>,
        headers: HttpHeaders,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.warn("Request validation failed fields={}", errors.map { it.field })
        val body = ErrorResponse(HttpStatus.BAD_REQUEST, VALIDATION_FAILED_MESSAGE, errors)
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request)
    }

    private fun fieldPath(ex: DatabindException): String? {
        val segments = ex.path.map { it.propertyName ?: "[${it.index}]" }.toMutableList()
        val missingProperty = (ex as? KotlinInvalidNullException)?.kotlinPropertyName
        if (missingProperty != null && segments.lastOrNull() != missingProperty) segments += missingProperty
        return segments.joinToString(".").replace(".[", "[").ifEmpty { null }
    }

    private fun clientMessage(status: HttpStatus) = when {
        status == HttpStatus.BAD_REQUEST -> "Nieprawidłowe żądanie."
        status == HttpStatus.NOT_FOUND -> "Nie znaleziono zasobu."
        status == HttpStatus.METHOD_NOT_ALLOWED -> "Ta operacja nie jest dostępna dla tego zasobu."
        status == HttpStatus.NOT_ACCEPTABLE || status == HttpStatus.UNSUPPORTED_MEDIA_TYPE -> "Nieobsługiwany format danych."
        status == HttpStatus.CONTENT_TOO_LARGE -> "Przesłane dane są zbyt duże."
        status.is4xxClientError -> "Nie można przetworzyć żądania."
        else -> status.reasonPhrase
    }

    private fun respond(
        status: HttpStatus,
        ex: Exception,
    ) = ResponseEntity.status(status).body(ErrorResponse(status, ex))

    companion object {
        private val log = logger()
        private const val VALIDATION_FAILED_MESSAGE = "Popraw błędy w formularzu."
        private const val REQUIRED_MESSAGE = "To pole jest wymagane."
        private const val INVALID_VALUE_MESSAGE = "Wartość ma nieprawidłowy format."
        private const val DUPLICATE_MESSAGE = "Taki zasób już istnieje."
    }
}
