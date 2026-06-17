package validation

// --- Combine independent validations — every error surfaces ---
//
// These are the heart of the library: combining validations concatenates the errors of
// each side instead of short-circuiting, so a single pass reports every failure. Use them
// to validate independent fields/elements. For a step that *depends* on an earlier result,
// reach for `andThen` (in ValidationSequencing.kt) instead.

/**
 * Combines two validations into a pair, accumulating errors from both if both are invalid.
 */
fun <E, A, B> Validation<E, A>.combine(other: Validation<E, B>): Validation<E, Pair<A, B>> =
    combine(other) { a, b -> a to b }

/**
 * Combines two validations with [transform], accumulating errors from both if both are invalid.
 */
inline fun <E, A, B, R> Validation<E, A>.combine(
    other: Validation<E, B>,
    transform: (A, B) -> R,
): Validation<E, R> = when (this) {
    is Valid -> when (other) {
        is Valid -> Valid(transform(value, other.value))
        is Invalid -> other
    }
    is Invalid -> when (other) {
        is Valid -> this
        is Invalid -> Invalid(this.errors + other.errors)
    }
}

// --- combine: assemble N independent validations into one, accumulating every error ---
// Type parameters use T1..Tn for the validated values and E for the error type.
// (Two validations are combined with the `Validation<E, A>.combine(other, transform)`
//  extension above; these top-level overloads cover arity 3 through 8.)

inline fun <E, T1, T2, T3, R> combine(
    v1: Validation<E, T1>,
    v2: Validation<E, T2>,
    v3: Validation<E, T3>,
    crossinline transform: (T1, T2, T3) -> R,
): Validation<E, R> = v1.combine(v2).combine(v3).map { (t12, t3) ->
    val (t1, t2) = t12
    transform(t1, t2, t3)
}

inline fun <E, T1, T2, T3, T4, R> combine(
    v1: Validation<E, T1>,
    v2: Validation<E, T2>,
    v3: Validation<E, T3>,
    v4: Validation<E, T4>,
    crossinline transform: (T1, T2, T3, T4) -> R,
): Validation<E, R> = v1.combine(v2).combine(v3).combine(v4).map { (t123, t4) ->
    val (t12, t3) = t123
    val (t1, t2) = t12
    transform(t1, t2, t3, t4)
}

inline fun <E, T1, T2, T3, T4, T5, R> combine(
    v1: Validation<E, T1>,
    v2: Validation<E, T2>,
    v3: Validation<E, T3>,
    v4: Validation<E, T4>,
    v5: Validation<E, T5>,
    crossinline transform: (T1, T2, T3, T4, T5) -> R,
): Validation<E, R> = v1.combine(v2).combine(v3).combine(v4).combine(v5).map { (t1234, t5) ->
    val (t123, t4) = t1234
    val (t12, t3) = t123
    val (t1, t2) = t12
    transform(t1, t2, t3, t4, t5)
}

inline fun <E, T1, T2, T3, T4, T5, T6, R> combine(
    v1: Validation<E, T1>,
    v2: Validation<E, T2>,
    v3: Validation<E, T3>,
    v4: Validation<E, T4>,
    v5: Validation<E, T5>,
    v6: Validation<E, T6>,
    crossinline transform: (T1, T2, T3, T4, T5, T6) -> R,
): Validation<E, R> = v1.combine(v2).combine(v3).combine(v4).combine(v5).combine(v6).map { (t12345, t6) ->
    val (t1234, t5) = t12345
    val (t123, t4) = t1234
    val (t12, t3) = t123
    val (t1, t2) = t12
    transform(t1, t2, t3, t4, t5, t6)
}

inline fun <E, T1, T2, T3, T4, T5, T6, T7, R> combine(
    v1: Validation<E, T1>,
    v2: Validation<E, T2>,
    v3: Validation<E, T3>,
    v4: Validation<E, T4>,
    v5: Validation<E, T5>,
    v6: Validation<E, T6>,
    v7: Validation<E, T7>,
    crossinline transform: (T1, T2, T3, T4, T5, T6, T7) -> R,
): Validation<E, R> = v1.combine(v2).combine(v3).combine(v4).combine(v5).combine(v6).combine(v7).map { (t123456, t7) ->
    val (t12345, t6) = t123456
    val (t1234, t5) = t12345
    val (t123, t4) = t1234
    val (t12, t3) = t123
    val (t1, t2) = t12
    transform(t1, t2, t3, t4, t5, t6, t7)
}

inline fun <E, T1, T2, T3, T4, T5, T6, T7, T8, R> combine(
    v1: Validation<E, T1>,
    v2: Validation<E, T2>,
    v3: Validation<E, T3>,
    v4: Validation<E, T4>,
    v5: Validation<E, T5>,
    v6: Validation<E, T6>,
    v7: Validation<E, T7>,
    v8: Validation<E, T8>,
    crossinline transform: (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
): Validation<E, R> =
    v1.combine(v2).combine(v3).combine(v4).combine(v5).combine(v6).combine(v7).combine(v8).map { (t1234567, t8) ->
        val (t123456, t7) = t1234567
        val (t12345, t6) = t123456
        val (t1234, t5) = t12345
        val (t123, t4) = t1234
        val (t12, t3) = t123
        val (t1, t2) = t12
        transform(t1, t2, t3, t4, t5, t6, t7, t8)
    }

// --- Collapse collections of validations, accumulating errors ---

/**
 * Turns a collection of validations into a validation of a list, accumulating every error.
 */
fun <E, A> Iterable<Validation<E, A>>.combineAll(): Validation<E, List<A>> {
    val values = mutableListOf<A>()
    val errors = mutableListOf<E>()
    for (v in this) when (v) {
        is Valid -> values.add(v.value)
        is Invalid -> errors.addAll(v.errors)
    }
    return if (errors.isEmpty()) Valid(values) else Invalid(errors)
}

/**
 * Validates each element with [validate] and accumulates all errors into one [Validation].
 */
inline fun <E, A, B> Iterable<A>.validateEach(validate: (A) -> Validation<E, B>): Validation<E, List<B>> {
    val values = mutableListOf<B>()
    val errors = mutableListOf<E>()
    for (a in this) when (val v = validate(a)) {
        is Valid -> values.add(v.value)
        is Invalid -> errors.addAll(v.errors)
    }
    return if (errors.isEmpty()) Valid(values) else Invalid(errors)
}
