package squad.nononotaa.backend.util

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import squad.nononotaa.backend.dto.CreateCaseRequest
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NarrowingIdentifierValidator::class])
annotation class NarrowingIdentifier(
    val message: String = "Podaj przynajmniej jeden identyfikator zawężający: pseudonim, login, adres e-mail, identyfikator w komunikatorze, adres profilu lub adres portfela kryptowalutowego. Samo imię i nazwisko nie wystarcza.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class NarrowingIdentifierValidator : ConstraintValidator<NarrowingIdentifier, CreateCaseRequest> {
    override fun isValid(
        value: CreateCaseRequest?,
        context: ConstraintValidatorContext,
    ): Boolean = value == null || listOf(value.nickname, value.login, value.email, value.messengerId, value.profileUrl, value.walletAddress).any { !it.isNullOrBlank() }
}
