package validation

// --- Transform a validated value ---

/**
 * Transforms the value if valid, otherwise returns the invalid instance unchanged.
 */
inline fun <E, A, B> Validation<E, A>.map(f: (A) -> B): Validation<E, B> = when (this) {
    is Valid -> Valid(f(value))
    is Invalid -> this
}

/**
 * Transforms the errors if invalid, otherwise returns the valid instance unchanged.
 */
inline fun <E1, E2, A> Validation<E1, A>.mapError(f: (E1) -> E2): Validation<E2, A> = when (this) {
    is Valid -> this
    is Invalid -> Invalid(errors.map(f))
}

/**
 * Returns the value if valid, otherwise the result of [default] applied to the errors.
 *
 * Declared as an extension (not a member) so the value type is resolved at the call site;
 * a member would erase it to `Nothing` on [Invalid] and fail at runtime.
 */
fun <E, A> Validation<E, A>.getOrElse(default: (List<E>) -> A): A = fold(default) { it }

// --- Interop ---

/**
 * Converts to a [Result], turning accumulated errors into a [Throwable] via [onErrors].
 */
fun <E, A> Validation<E, A>.toResult(onErrors: (List<E>) -> Throwable): Result<A> =
    fold({ Result.failure(onErrors(it)) }, { Result.success(it) })
