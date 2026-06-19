package validation.examples

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RegistrationTest {

    @Test
    fun buildsAccountFromValidForm() {
        val result = RegistrationForm(
            email = "alice@example.com",
            password = "s3cret-pass",
            passwordConfirmation = "s3cret-pass",
            age = 30,
            termsAccepted = true,
        ).validate()
        assertEquals(Ok(Account(Email("alice@example.com"), 30)), result)
    }

    @Test
    fun accumulatesEveryFormError() {
        val result = RegistrationForm(
            email = "not-an-email",
            password = "short",
            passwordConfirmation = "different",
            age = 15,
            termsAccepted = false,
        ).validate()
        assertEquals(
            setOf(
                RegistrationError.MalformedEmail("not-an-email"),
                RegistrationError.PasswordTooShort(5),
                RegistrationError.PasswordTooWeak,
                RegistrationError.PasswordMismatch,
                RegistrationError.Underage(15),
                RegistrationError.TermsNotAccepted,
            ),
            result.getError().orEmpty().toSet(),
        )
    }
}
