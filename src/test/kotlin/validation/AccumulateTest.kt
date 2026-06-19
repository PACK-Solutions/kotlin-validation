package validation

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AccumulateTest {

    private fun nonBlank(value: String): Validated<String, String> =
        if (value.isNotBlank()) valid(value) else invalid("blank")

    private fun positive(value: Int): Validated<String, Int> =
        if (value > 0) valid(value) else invalid("non-positive:$value")

    @Test
    fun accumulateReturnsOkWhenAllValid() {
        val result = accumulate(nonBlank("a"), positive(2)) { s, n -> "$s$n" }
        assertEquals(Ok("a2"), result)
    }

    @Test
    fun accumulateConcatenatesAllErrors() {
        val result = accumulate(nonBlank(""), positive(-1), nonBlank("ok")) { a, b, c -> "$a$b$c" }
        assertEquals(Err(listOf("blank", "non-positive:-1")), result)
    }

    @Test
    fun accumulateSupportsHighArity() {
        // six inputs — above kotlin-result's native zipOrAccumulate limit of five
        val result = accumulate(
            positive(1), positive(2), positive(3), positive(-4), positive(5), positive(-6),
        ) { a, b, c, d, e, f -> a + b + c + d + e + f }
        assertEquals(Err(listOf("non-positive:-4", "non-positive:-6")), result)
    }

    @Test
    fun validateEachAccumulatesPerElementErrors() {
        val result = listOf("a", "", "b", "").validateEach(::nonBlank)
        assertEquals(Err(listOf("blank", "blank")), result)
    }

    @Test
    fun validateEachReturnsAllValuesWhenValid() {
        val result = listOf("a", "b").validateEach(::nonBlank)
        assertEquals(Ok(listOf("a", "b")), result)
    }

    @Test
    fun combineAllFoldsListOfResults() {
        val results: List<Validated<String, Int>> = listOf(positive(1), positive(-2), positive(-3))
        assertEquals(Err(listOf("non-positive:-2", "non-positive:-3")), results.combineAll())
    }

    @Test
    fun validatedBuilderAccumulatesChecksAndNestedErrors() {
        val result: Validated<String, Int> = validated {
            addErrorsOf(nonBlank(""))
            check(false, "check-failed")
            build { 42 }
        }
        assertEquals(Err(listOf("blank", "check-failed")), result)
    }

    @Test
    fun validatedBuilderBuildsWhenNoErrors() {
        val result: Validated<String, Int> = validated {
            addErrorsOf(nonBlank("ok"))
            check(true, "never")
            build { 42 }
        }
        assertEquals(Ok(42), result)
    }
}
