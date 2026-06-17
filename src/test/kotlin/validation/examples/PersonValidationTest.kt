package validation.examples

import org.junit.jupiter.api.Test
import validation.Invalid
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonValidationTest {

    @Test
    fun buildsValidPersonFromUnvalidatedInput() {
        val raw = UnvalidatedPerson(
            firstName = "Marie",
            lastName = "Dubois",
            birth = LocalDate.of(1990, 2, 21),
            civilStatus = "mrs", // case-insensitive parse
            socialSecurityNumber = "2 90 02 21 999 999 99",
            nationalities = listOf("FR", "be"), // normalized to upper-case
        )

        val result = raw.validate()

        assertTrue(result.isValid)
        val person = result.getOrNull()!!
        assertEquals("Marie", person.firstName)
        assertEquals(BirthDate(LocalDate.of(1990, 2, 21)), person.birth)
        assertEquals(CivilStatus.MRS, person.civilStatus)
        assertEquals(listOf(CountryCode("FR"), CountryCode("BE")), person.nationalities)
    }

    @Test
    fun accumulatesEveryFieldError() {
        val raw = UnvalidatedPerson(
            firstName = "A".repeat(51),
            lastName = "B".repeat(61),
            birth = LocalDate.of(1850, 1, 1),
            civilStatus = "X",
            socialSecurityNumber = "",
            nationalities = emptyList(),
        )

        val result = raw.validate()

        assertTrue(result.isInvalid)
        assertEquals(
            setOf(
                PersonError.FirstNameTooLong(51),
                PersonError.LastNameTooLong(61),
                PersonError.BirthDateTooEarly(LocalDate.of(1850, 1, 1)),
                PersonError.UnknownCivilStatus("X"),
                PersonError.InvalidSocialSecurityNumber(""),
                PersonError.NationalitiesEmpty,
            ),
            result.errorsOrEmpty().toSet(),
        )
    }

    @Test
    fun accumulatesEveryInvalidCountryCodeViaValidateEach() {
        val raw = UnvalidatedPerson(
            firstName = "Marie",
            lastName = "Dubois",
            birth = LocalDate.of(1990, 2, 21),
            civilStatus = "MRS",
            socialSecurityNumber = "valid-ssn",
            nationalities = listOf("FRA", "X", "BE"), // two invalid codes
        )

        val result = raw.validate()

        assertEquals(
            Invalid(listOf(PersonError.InvalidCountryCode("FRA"), PersonError.InvalidCountryCode("X"))),
            result,
        )
    }

    @Test
    fun dslCreateReusesFieldValidatorsAndAccumulates() {
        val result = Person.create(
            firstName = "A".repeat(51),
            lastName = "Dubois",
            birth = BirthDate(LocalDate.of(1990, 2, 21)),
            civilStatus = CivilStatus.MR,
            socialSecurityNumber = "",
            nationalities = emptyList(),
        )
        assertEquals(
            setOf(
                PersonError.FirstNameTooLong(51),
                PersonError.InvalidSocialSecurityNumber(""),
                PersonError.NationalitiesEmpty,
            ),
            result.errorsOrEmpty().toSet(),
        )
    }
}
