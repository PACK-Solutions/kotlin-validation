package validation.examples

import validation.Validation
import validation.invalid
import validation.valid
import validation.validation

/**
 * Exemple : validation d'un formulaire d'inscription.
 *
 * Domaine familier de validation de formulaire. Utilise le DSL [validation] : chaque règle
 * (email, robustesse du mot de passe, âge, conditions, confirmation) est contrôlée et toutes
 * les erreurs sont accumulées avant de construire le compte.
 */

/** Adresse email validée. */
@JvmInline
value class Email(val value: String)

/** Compte d'utilisateur validé, conforme par construction. */
data class Account(val email: Email, val age: Int)

/** Formulaire d'inscription brut. */
data class RegistrationForm(
    val email: String,
    val password: String,
    val passwordConfirmation: String,
    val age: Int,
    val termsAccepted: Boolean,
)

/** Hiérarchie d'erreurs pour la validation d'une inscription. */
sealed interface RegistrationError {
    data class MalformedEmail(val value: String) : RegistrationError
    data class PasswordTooShort(val length: Int) : RegistrationError
    data object PasswordTooWeak : RegistrationError
    data object PasswordMismatch : RegistrationError
    data class Underage(val age: Int) : RegistrationError
    data object TermsNotAccepted : RegistrationError
}

/** Longueur minimale du mot de passe. */
private const val MIN_PASSWORD_LENGTH = 8

/** Âge minimum légal pour s'inscrire. */
private const val MINIMUM_AGE = 18

private fun isStrong(password: String): Boolean =
    password.any(Char::isDigit) && password.any(Char::isLetter)

/**
 * Valide un formulaire d'inscription et produit un [Account] conforme.
 * Accumule toutes les erreurs en un seul passage.
 */
fun RegistrationForm.validate(): Validation<RegistrationError, Account> = validation {
    addErrorsOf(validateEmail(email))

    check(password.length >= MIN_PASSWORD_LENGTH, RegistrationError.PasswordTooShort(password.length))
    check(isStrong(password), RegistrationError.PasswordTooWeak)
    check(password == passwordConfirmation, RegistrationError.PasswordMismatch)
    check(age >= MINIMUM_AGE, RegistrationError.Underage(age))
    check(termsAccepted, RegistrationError.TermsNotAccepted)

    build { Account(Email(email), age) }
}

private fun validateEmail(value: String): Validation<RegistrationError, Email> {
    val wellFormed = value.count { it == '@' } == 1 &&
        value.substringBefore('@').isNotBlank() &&
        value.substringAfter('@').let { it.contains('.') && !it.startsWith('.') && !it.endsWith('.') }
    return if (wellFormed) valid(Email(value)) else invalid(RegistrationError.MalformedEmail(value))
}
