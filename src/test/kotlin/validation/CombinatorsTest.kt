package validation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombinatorsTest {

    private val parseInt: (String) -> Validation<String, Int> =
        { s -> s.toIntOrNull()?.let(::valid) ?: invalid("not a number: $s") }

    @Test
    fun andThenChainsDependentValidations() {
        assertEquals(Valid(42), valid("42").andThen(parseInt))
        assertEquals(Invalid(listOf("not a number: x")), valid("x").andThen(parseInt))
        // fail-fast: an upstream error short-circuits and next is never run
        val upstream: Validation<String, String> = invalid("upstream")
        assertEquals(Invalid(listOf("upstream")), upstream.andThen(parseInt))
    }

    @Test
    fun orElseRecoversOnlyWhenInvalid() {
        val invalid: Validation<String, Int> = invalid("e")
        assertEquals(Valid(0), invalid.orElse { valid(0) })
        assertEquals(Valid(1), valid(1).orElse { valid(99) })
    }

    @Test
    fun combinePairAccumulates() {
        assertEquals(Valid(3), valid(1).combine(valid(2)) { a, b -> a + b })
        val both = invalid<String>("a").combine(invalid("b")) { _: Nothing, _: Nothing -> 0 }
        assertEquals(Invalid(listOf("a", "b")), both)
    }

    @Test
    fun combineHigherArityAccumulatesEveryError() {
        val ok = combine(valid(1), valid(2), valid(3), valid(4), valid(5), valid(6)) { a, b, c, d, e, f ->
            a + b + c + d + e + f
        }
        assertEquals(Valid(21), ok)

        val errs = combine(
            invalid<String>("e1"), valid(2), invalid("e3"), valid(4), valid(5), valid(6),
        ) { _, _, _, _, _, _ -> 0 }
        assertEquals(Invalid(listOf("e1", "e3")), errs)
    }

    @Test
    fun combineAllAccumulates() {
        assertEquals(Valid(listOf(1, 2, 3)), listOf(valid(1), valid(2), valid(3)).combineAll())
        assertEquals(
            Invalid(listOf("e1", "e2")),
            listOf(valid(1), invalid<String>("e1"), invalid("e2")).combineAll(),
        )
    }

    @Test
    fun validateEachAccumulates() {
        assertEquals(Valid(listOf(1, 2, 3)), listOf("1", "2", "3").validateEach(parseInt))
        assertEquals(
            Invalid(listOf("not a number: x", "not a number: y")),
            listOf("1", "x", "y").validateEach(parseInt),
        )
    }

    @Test
    fun toResultConverts() {
        assertEquals(42, valid(42).toResult { IllegalStateException() }.getOrNull())
        val failure = invalid<String>("e").toResult { IllegalStateException(it.toString()) }
        assertTrue(failure.isFailure)
    }

    @Test
    fun getOrElseReturnsTypedValue() {
        val invalid: Validation<String, Int> = invalid("e")
        assertEquals(-1, invalid.getOrElse { -1 })
        assertEquals(42, valid(42).getOrElse { -1 })
    }
}
