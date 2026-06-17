package validation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ValidatorTest {

    private val notBlank = validator<String, String>({ "blank" }) { it.isNotBlank() }
    private val maxLen5 = validator<String, String>({ "too long: ${it.length}" }) { it.length <= 5 }
    private val containsA = validator<String, String>({ "no 'a'" }) { it.contains("a") }

    @Test
    fun validatorPassesAndFails() {
        assertEquals(Valid("ok"), notBlank("ok"))
        assertEquals(Invalid(listOf("blank")), notBlank(""))
    }

    @Test
    fun andRunsBothAndAccumulates() {
        val v = maxLen5 and containsA
        assertEquals(Valid("abc"), v("abc"))
        // both rules fail on the same value -> both errors collected
        assertEquals(Invalid(listOf("too long: 6", "no 'a'")), v("xxxxxx"))
    }

    @Test
    fun allOfCombinesEveryValidator() {
        val v = allOf(notBlank, maxLen5, containsA)
        assertEquals(Valid("abc"), v("abc"))
        assertEquals(Invalid(listOf("too long: 6", "no 'a'")), v("123456"))
    }

    @Test
    fun allOfWithNoValidatorsIsAlwaysValid() {
        val v = allOf<String, String>()
        assertEquals(Valid("anything"), v("anything"))
    }
}
