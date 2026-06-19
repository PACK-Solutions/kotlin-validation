package validation.examples

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OrderValidationTest {

    private val goodAddress = UnvalidatedShippingAddress("12 rue des Lilas", "Lyon", "69003", "FR")

    @Test
    fun buildsValidOrder() {
        val raw = UnvalidatedOrder(
            lines = listOf(UnvalidatedOrderLine("SKU-1", 2, 1990), UnvalidatedOrderLine("SKU-2", 1, 500)),
            shipping = goodAddress,
            currency = "eur", // normalized to upper-case
        )

        val result = raw.validate()

        val order = assertNotNull(result.get())
        assertEquals(2, order.lines.size)
        assertEquals(CurrencyCode("EUR"), order.currency)
        assertEquals(CountryCode("FR"), order.shipping.country)
    }

    @Test
    fun reportsEveryBadLineAndField() {
        val raw = UnvalidatedOrder(
            lines = listOf(UnvalidatedOrderLine("SKU-1", 0, -100), UnvalidatedOrderLine("", 3, 200)),
            shipping = UnvalidatedShippingAddress("", "Lyon", "69003", "FRA"),
            currency = "XXX",
        )

        val result = raw.validate()

        assertEquals(
            setOf(
                OrderError.NonPositiveQuantity("SKU-1", 0),
                OrderError.NegativePrice("SKU-1", -100),
                OrderError.BlankSku(3),
                OrderError.BlankAddressField("street"),
                OrderError.InvalidShippingCountry("FRA"),
                OrderError.UnsupportedCurrency("XXX"),
            ),
            result.getError().orEmpty().toSet(),
        )
    }

    @Test
    fun rejectsEmptyCart() {
        val raw = UnvalidatedOrder(lines = emptyList(), shipping = goodAddress, currency = "EUR")
        assertEquals(listOf(OrderError.EmptyCart), raw.validate().getError().orEmpty())
    }
}
