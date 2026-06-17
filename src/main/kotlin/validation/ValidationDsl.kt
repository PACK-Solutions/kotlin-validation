package validation

/**
 * Scope for accumulating validation errors and building a final [Validation] result.
 *
 * Open one with [validation]. Use [check]/[ensure] for inline predicate checks,
 * [addError]/[addErrors] to record errors directly, and [addErrorsOf] to fold a nested
 * [Validation]'s errors into the scope. [build] produces the final result:
 * [Valid] only if no errors were recorded, otherwise [Invalid] with everything collected.
 */
class ValidationScope<E> {
    private val _errors = mutableListOf<E>()

    /**
     * Records [error] if [condition] is false.
     */
    fun check(condition: Boolean, error: E) {
        if (!condition) _errors.add(error)
    }

    /**
     * Alias for [check], reading as a precondition.
     */
    fun ensure(condition: Boolean, error: E) = check(condition, error)

    /**
     * Adds an error to the accumulation.
     */
    fun addError(error: E) {
        _errors.add(error)
    }

    /**
     * Adds multiple errors to the accumulation.
     */
    fun addErrors(errors: List<E>) {
        _errors.addAll(errors)
    }

    /**
     * Folds a nested [validation]'s errors into the scope (does nothing if it is valid).
     * Use it to reuse an existing validator inside the builder; the validated value itself
     * is rebuilt in [build], which only runs once no error has been recorded.
     */
    fun addErrorsOf(validation: Validation<E, *>) {
        if (validation is Invalid) _errors.addAll(validation.errors)
    }

    /**
     * Builds the final [Validation]: [Valid] of [value] if no errors were recorded,
     * otherwise [Invalid] with the accumulated errors.
     */
    fun <A> build(value: () -> A): Validation<E, A> =
        if (_errors.isEmpty()) Valid(value()) else Invalid(_errors.toList())
}

/**
 * Creates a [ValidationScope] and runs [block] to produce a [Validation].
 */
fun <E, A> validation(block: ValidationScope<E>.() -> Validation<E, A>): Validation<E, A> =
    ValidationScope<E>().block()
