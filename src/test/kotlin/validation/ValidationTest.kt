// test/ValidationTest.kt
package validation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ValidationTest {

    @Test
    fun testValid() {
        val v = valid(42)
        assertEquals(Valid(42), v)
    }

    @Test
    fun testInvalid() {
        val i = invalid("error")
        assertEquals(Invalid(listOf("error")), i)
        
        val multi = invalid(listOf("e1", "e2"))
        assertEquals(Invalid(listOf("e1", "e2")), multi)
    }

    @Test
    fun testMap() {
        val v = valid(21).map { it * 2 }
        assertEquals(Valid(42), v)
        
        val i: Validation<String, Int> = invalid("error")
        val i2 = i.map { it * 2 }
        assertEquals(i, i2)
    }

    @Test
    fun testMapError() {
        val v: Validation<String, Int> = valid(42)
        val v2 = v.mapError { it.uppercase() }
        assertEquals(v, v2)
        
        val i = invalid("error").mapError { it.uppercase() }
        assertEquals(Invalid(listOf("ERROR")), i)
    }

    @Test
    fun testCombinePair() {
        val v1 = valid(1)
        val v2 = valid("a")
        assertEquals(Valid(1 to "a"), v1.combine(v2))

        val i1 = invalid("e1")
        assertEquals(i1, i1.combine(v2))
        assertEquals(i1, v1.combine(i1))

        val i2 = invalid("e2")
        assertEquals(Invalid(listOf("e1", "e2")), i1.combine(i2))
    }

    @Test
    fun testCombine() {
        val v1 = valid(1)
        val v2 = valid(2)
        val res = v1.combine(v2) { a, b -> a + b }
        assertEquals(Valid(3), res)

        val v3 = valid(3)
        val res3 = combine(v1, v2, v3) { a, b, c -> a + b + c }
        assertEquals(Valid(6), res3)
    }

    @Test
    fun testFold() {
        val v = valid(42)
        val res = v.fold({ 0 }, { it })
        assertEquals(42, res)
        
        val i = invalid("error")
        val res2 = i.fold({ it.size }, { 0 })
        assertEquals(1, res2)
    }

    @Test
    fun testProperties() {
        val v = valid(1)
        assertEquals(true, v.isValid)
        assertEquals(false, v.isInvalid)
        
        val i = invalid("e")
        assertEquals(false, i.isValid)
        assertEquals(true, i.isInvalid)
    }

    @Test
    fun testDSL() {
        val res = validation<String, Int> {
            check(true, "never")
            check(false, "error1")
            addError("error2")
            addErrorsOf(invalid("error3"))
            build { 42 }
        }

        assertEquals(Invalid(listOf("error1", "error2", "error3")), res)

        val success = validation<String, Int> {
            addErrorsOf(valid(10)) // valid -> nothing recorded
            build { 42 }
        }
        assertEquals(Valid(42), success)
    }
}
