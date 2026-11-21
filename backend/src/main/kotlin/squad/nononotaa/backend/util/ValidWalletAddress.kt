package squad.nononotaa.backend.util

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.AddressValidationFailure
import squad.nononotaa.backend.service.AddressValidator
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [WalletAddressValidator::class])
annotation class ValidWalletAddress(
    val message: String = "Podaj poprawny adres portfela Bitcoin (BTC) lub Ethereum (ETH).",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class WalletAddressValidator : ConstraintValidator<ValidWalletAddress, String?> {
    private val addressValidator = AddressValidator()

    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value.isNullOrBlank()) return true
        val result = addressValidator.validate(value)
        if (result is AddressValidation.Valid) return true
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(messageFor((result as AddressValidation.Invalid).failure)).addConstraintViolation()
        return false
    }

    private fun messageFor(failure: AddressValidationFailure) = when (failure) {
        AddressValidationFailure.MALFORMED -> "Adres portfela ma nieprawidłowy format."
        AddressValidationFailure.CHECKSUM_MISMATCH -> "Adres portfela ma nieprawidłową sumę kontrolną."
        AddressValidationFailure.UNSUPPORTED_NETWORK -> "Nieobsługiwany format adresu portfela. Obsługiwane są adresy Bitcoin (BTC) i Ethereum (ETH)."
        AddressValidationFailure.NETWORK_MISMATCH -> "Adres portfela nie pasuje do wskazanej sieci."
    }
}
