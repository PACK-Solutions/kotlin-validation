package validation.examples

import validation.Validated
import validation.accumulate
import validation.invalid
import validation.valid
import validation.validateEach
import validation.validated
import java.time.LocalDate

/**
 * Représente le statut civil (Monsieur ou Madame).
 */
enum class CivilStatus {
    MR, MRS
}

/**
 * Représente la date de naissance.
 */
@JvmInline
value class BirthDate(val date: LocalDate)

/**
 * Représente un code pays (ISO 3166-1 alpha-2 par exemple).
 */
@JvmInline
value class CountryCode(val code: String)

/**
 * Personne validée, garantie conforme par construction.
 */
data class Person(
    val firstName: String,
    val lastName: String,
    val birth: BirthDate,
    val civilStatus: CivilStatus,
    val socialSecurityNumber: String,
    val nationalities: List<CountryCode>,
) {
    companion object {
        /**
         * Construit une [Person] à partir de champs déjà typés, via le builder [validated].
         *
         * Réutilise les mêmes validateurs de champ que [UnvalidatedPerson.validate] : les
         * règles (longueurs, numéro de sécurité sociale) ne sont définies qu'à un seul endroit.
         * Accumule toutes les erreurs avant de produire le résultat.
         */
        fun create(
            firstName: String,
            lastName: String,
            birth: BirthDate,
            civilStatus: CivilStatus,
            socialSecurityNumber: String,
            nationalities: List<CountryCode>,
        ): Validated<PersonError, Person> = validated {
            addErrorsOf(validateFirstName(firstName))
            addErrorsOf(validateLastName(lastName))
            addErrorsOf(validateSocialSecurityNumber(socialSecurityNumber))
            check(nationalities.isNotEmpty(), PersonError.NationalitiesEmpty)
            build {
                Person(firstName, lastName, birth, civilStatus, socialSecurityNumber, nationalities)
            }
        }
    }
}

/**
 * Données brutes non validées (issues par exemple d'un formulaire ou d'un JSON).
 * Chaque champ est un type primitif tant qu'il n'a pas été validé.
 */
data class UnvalidatedPerson(
    val firstName: String,
    val lastName: String,
    val birth: LocalDate,
    val civilStatus: String,
    val socialSecurityNumber: String,
    val nationalities: List<String>,
)

/**
 * Hiérarchie d'erreurs pour la validation d'une Personne.
 */
sealed interface PersonError {
    data class FirstNameTooLong(val length: Int) : PersonError
    data class LastNameTooLong(val length: Int) : PersonError
    data object NationalitiesEmpty : PersonError
    data class InvalidSocialSecurityNumber(val value: String) : PersonError
    data class UnknownCivilStatus(val value: String) : PersonError
    data class InvalidCountryCode(val value: String) : PersonError
    data class BirthDateTooEarly(val date: LocalDate) : PersonError
}

/** Date de naissance la plus ancienne acceptée. */
private val EARLIEST_BIRTH: LocalDate = LocalDate.of(1900, 1, 1)

private fun validateFirstName(value: String): Validated<PersonError, String> =
    if (value.length <= 50) valid(value) else invalid(PersonError.FirstNameTooLong(value.length))

private fun validateLastName(value: String): Validated<PersonError, String> =
    if (value.length <= 60) valid(value) else invalid(PersonError.LastNameTooLong(value.length))

private fun validateSocialSecurityNumber(value: String): Validated<PersonError, String> =
    if (value.isNotBlank()) valid(value) else invalid(PersonError.InvalidSocialSecurityNumber(value))

private fun validateCivilStatus(value: String): Validated<PersonError, CivilStatus> =
    CivilStatus.entries.find { it.name == value.uppercase() }
        ?.let { valid(it) }
        ?: invalid(PersonError.UnknownCivilStatus(value))

private fun validateBirth(value: LocalDate): Validated<PersonError, BirthDate> =
    if (!value.isBefore(EARLIEST_BIRTH)) valid(BirthDate(value))
    else invalid(PersonError.BirthDateTooEarly(value))

private fun validateCountryCode(value: String): Validated<PersonError, CountryCode> =
    if (value.length == 2 && value.all(Char::isLetter)) valid(CountryCode(value.uppercase()))
    else invalid(PersonError.InvalidCountryCode(value))

private fun validateNationalities(values: List<String>): Validated<PersonError, List<CountryCode>> =
    if (values.isEmpty()) invalid(PersonError.NationalitiesEmpty)
    else values.validateEach(::validateCountryCode)

/**
 * Valide chaque champ indépendamment puis assemble une [Person] via [accumulate].
 *
 * Toutes les erreurs des différents champs (y compris chaque code pays invalide,
 * grâce à [validateEach]) sont accumulées en un seul passage. Six champs sont combinés ici —
 * au-delà de l'arité 5 du `zipOrAccumulate` natif de kotlin-result, d'où le helper [accumulate].
 */
fun UnvalidatedPerson.validate(): Validated<PersonError, Person> = accumulate(
    validateFirstName(firstName),
    validateLastName(lastName),
    validateBirth(birth),
    validateCivilStatus(civilStatus),
    validateSocialSecurityNumber(socialSecurityNumber),
    validateNationalities(nationalities),
) { firstName, lastName, birth, civilStatus, ssn, nationalities ->
    Person(firstName, lastName, birth, civilStatus, ssn, nationalities)
}
