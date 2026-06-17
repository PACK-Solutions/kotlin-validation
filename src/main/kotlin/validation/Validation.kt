package validation

/**
 * Core sealed interface for error-accumulating validation.
 *
 * A [Validation] holds either a list of accumulated errors of type [E] ([Invalid])
 * or a single valid value of type [A] ([Valid]). Unlike a fail-fast `Result`/`Either`,
 * combining several [Validation]s (see `combine`, `combineAll`, `validateEach`)
 * concatenates their errors so every failure surfaces in a single pass.
 */
sealed interface Validation<out E, out A> {

    /**
     * Folds the validation into a single value of type [R].
     */
    fun <R> fold(ifInvalid: (List<E>) -> R, ifValid: (A) -> R): R = when (this) {
        is Valid -> ifValid(value)
        is Invalid -> ifInvalid(errors)
    }

    /**
     * Returns the value if valid, otherwise null.
     */
    fun getOrNull(): A? = fold({ null }, { it })

    /**
     * Returns the accumulated errors if invalid, otherwise an empty list.
     */
    fun errorsOrEmpty(): List<E> = fold({ it }, { emptyList() })

    /**
     * Executes [action] if the validation is valid, then returns this unchanged.
     */
    fun onValid(action: (A) -> Unit): Validation<E, A> = also { if (it is Valid) action(it.value) }

    /**
     * Executes [action] if the validation is invalid, then returns this unchanged.
     */
    fun onInvalid(action: (List<E>) -> Unit): Validation<E, A> = also { if (it is Invalid) action(it.errors) }

    /**
     * Returns true if valid.
     */
    val isValid: Boolean get() = this is Valid

    /**
     * Returns true if invalid.
     */
    val isInvalid: Boolean get() = this is Invalid
}

/**
 * Represents a successful validation containing a value of type [A].
 */
data class Valid<out A>(val value: A) : Validation<Nothing, A>

/**
 * Represents a failed validation containing a list of accumulated errors of type [E].
 */
data class Invalid<out E>(val errors: List<E>) : Validation<E, Nothing>

/**
 * Helper constructor for [Valid].
 */
fun <A> valid(value: A): Validation<Nothing, A> = Valid(value)

/**
 * Helper constructor for [Invalid] with a single error.
 */
fun <E> invalid(error: E): Validation<E, Nothing> = Invalid(listOf(error))

/**
 * Helper constructor for [Invalid] with multiple errors.
 */
fun <E> invalid(errors: List<E>): Validation<E, Nothing> = Invalid(errors)

/**
 * Lifts a nullable value into a [Validation], producing [error] when the value is null.
 */
fun <E, A> A?.toValidation(error: () -> E): Validation<E, A> =
    if (this != null) Valid(this) else invalid(error())
