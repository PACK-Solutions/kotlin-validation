package validation

/**
 * A reusable validation rule: takes an input of type [A] and returns it unchanged
 * when valid, or the accumulated errors of type [E] when invalid.
 */
typealias Validator<E, A> = (A) -> Validation<E, A>

/**
 * Builds a [Validator] from a [predicate]; produces [error] (derived from the input)
 * when the predicate fails.
 */
fun <E, A> validator(error: (A) -> E, predicate: (A) -> Boolean): Validator<E, A> = { a ->
    if (predicate(a)) Valid(a) else invalid(error(a))
}

/**
 * Combines two validators on the same value, accumulating errors from both.
 */
infix fun <E, A> Validator<E, A>.and(other: Validator<E, A>): Validator<E, A> = { a ->
    this(a).combine(other(a)) { value, _ -> value }
}

/**
 * Combines any number of validators on the same value, accumulating all their errors.
 * With no validators the value is always [Valid].
 */
fun <E, A> allOf(vararg validators: Validator<E, A>): Validator<E, A> = { a ->
    validators.map { it(a) }.combineAll().map { a }
}
