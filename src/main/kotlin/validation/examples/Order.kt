package validation.examples

import validation.Validation
import validation.combine
import validation.invalid
import validation.valid
import validation.validateEach

/**
 * Exemple : validation d'une commande e-commerce.
 *
 * Illustre l'accumulation pure : chaque ligne fautive est signalée (via [validateEach]),
 * et tous les défauts (panier, adresse, devise) remontent en un seul passage (via [combine]).
 */

/** Référence article (SKU). */
@JvmInline
value class Sku(val value: String)

/** Code devise ISO 4217 (EUR, USD, ...). */
@JvmInline
value class CurrencyCode(val code: String)

/** Ligne de commande validée. */
data class OrderLine(val sku: Sku, val quantity: Int, val unitPriceCents: Long)

/** Adresse de livraison validée. */
data class ShippingAddress(
    val street: String,
    val city: String,
    val postalCode: String,
    val country: CountryCode,
)

/** Commande validée, conforme par construction. */
data class Order(
    val lines: List<OrderLine>,
    val shipping: ShippingAddress,
    val currency: CurrencyCode,
)

/** Ligne de commande brute (avant validation). */
data class UnvalidatedOrderLine(val sku: String, val quantity: Int, val unitPriceCents: Long)

/** Adresse de livraison brute (avant validation). */
data class UnvalidatedShippingAddress(
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
)

/** Commande brute (issue par exemple d'un panier ou d'un appel API). */
data class UnvalidatedOrder(
    val lines: List<UnvalidatedOrderLine>,
    val shipping: UnvalidatedShippingAddress,
    val currency: String,
)

/** Hiérarchie d'erreurs pour la validation d'une commande. */
sealed interface OrderError {
    data object EmptyCart : OrderError
    data class BlankSku(val quantity: Int) : OrderError
    data class NonPositiveQuantity(val sku: String, val quantity: Int) : OrderError
    data class NegativePrice(val sku: String, val unitPriceCents: Long) : OrderError
    data class UnsupportedCurrency(val code: String) : OrderError
    data class BlankAddressField(val field: String) : OrderError
    data class InvalidShippingCountry(val value: String) : OrderError
}

/** Devises acceptées par la boutique. */
private val SUPPORTED_CURRENCIES = setOf("EUR", "USD", "GBP")

private fun validateLine(line: UnvalidatedOrderLine): Validation<OrderError, OrderLine> = combine(
    validateSku(line),
    validateQuantity(line),
    validatePrice(line),
) { sku, quantity, price -> OrderLine(sku, quantity, price) }

private fun validateSku(line: UnvalidatedOrderLine): Validation<OrderError, Sku> =
    if (line.sku.isNotBlank()) valid(Sku(line.sku)) else invalid(OrderError.BlankSku(line.quantity))

private fun validateQuantity(line: UnvalidatedOrderLine): Validation<OrderError, Int> =
    if (line.quantity > 0) valid(line.quantity)
    else invalid(OrderError.NonPositiveQuantity(line.sku, line.quantity))

private fun validatePrice(line: UnvalidatedOrderLine): Validation<OrderError, Long> =
    if (line.unitPriceCents >= 0) valid(line.unitPriceCents)
    else invalid(OrderError.NegativePrice(line.sku, line.unitPriceCents))

private fun validateLines(lines: List<UnvalidatedOrderLine>): Validation<OrderError, List<OrderLine>> =
    if (lines.isEmpty()) invalid(OrderError.EmptyCart)
    else lines.validateEach(::validateLine)

private fun validateCurrency(code: String): Validation<OrderError, CurrencyCode> =
    if (code.uppercase() in SUPPORTED_CURRENCIES) valid(CurrencyCode(code.uppercase()))
    else invalid(OrderError.UnsupportedCurrency(code))

private fun validateAddress(address: UnvalidatedShippingAddress): Validation<OrderError, ShippingAddress> = combine(
    requireNotBlank(address.street, "street"),
    requireNotBlank(address.city, "city"),
    requireNotBlank(address.postalCode, "postalCode"),
    validateShippingCountry(address.country),
) { street, city, postalCode, country -> ShippingAddress(street, city, postalCode, country) }

private fun requireNotBlank(value: String, field: String): Validation<OrderError, String> =
    if (value.isNotBlank()) valid(value) else invalid(OrderError.BlankAddressField(field))

private fun validateShippingCountry(value: String): Validation<OrderError, CountryCode> =
    if (value.length == 2 && value.all(Char::isLetter)) valid(CountryCode(value.uppercase()))
    else invalid(OrderError.InvalidShippingCountry(value))

/**
 * Valide une commande : panier non vide, chaque ligne, l'adresse et la devise.
 * Toutes les erreurs sont accumulées en un seul passage.
 */
fun UnvalidatedOrder.validate(): Validation<OrderError, Order> = combine(
    validateLines(lines),
    validateAddress(shipping),
    validateCurrency(currency),
) { lines, shipping, currency -> Order(lines, shipping, currency) }
