package validation

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError

/**
 * Error-accumulating validation, carried by [kotlin-result](https://github.com/michaelbull/kotlin-result).
 *
 * The carrier is kotlin-result's `Result<V, E>` — note the order **value first, error second**
 * (the opposite of a `Validation<E, A>` signature). The error branch holds a `List<E>` so that
 * multiple errors **accumulate** instead of short-circuiting at the first one.
 *
 * kotlin-result already provides the *fail-fast* / dependent path (`andThen`), recovery
 * (`recover`, `getOr`/`getOrElse`) and transformations (`map`, `mapError`, `fold`). This file
 * adds the thin **accumulation** layer the library lacks: [accumulate] (combine independent
 * fields), [validateEach] / [combineAll] (collections) and the imperative [validated] builder.
 */
typealias Validated<E, A> = Result<A, List<E>>

/** Builds a success. */
fun <E, A> valid(value: A): Validated<E, A> = Ok(value)

/** Builds a failure carrying a single error. */
fun <E> invalid(error: E): Validated<E, Nothing> = Err(listOf(error))

// region accumulate — combine N independent validations, accumulating every error

inline fun <E, T1, T2, R> accumulate(
    v1: Validated<E, T1>,
    v2: Validated<E, T2>,
    transform: (T1, T2) -> R,
): Validated<E, R> {
    val errors = buildList {
        v1.getError()?.let(::addAll)
        v2.getError()?.let(::addAll)
    }
    return if (errors.isEmpty()) Ok(transform(v1.get()!!, v2.get()!!)) else Err(errors)
}

inline fun <E, T1, T2, T3, R> accumulate(
    v1: Validated<E, T1>,
    v2: Validated<E, T2>,
    v3: Validated<E, T3>,
    transform: (T1, T2, T3) -> R,
): Validated<E, R> {
    val errors = buildList {
        v1.getError()?.let(::addAll)
        v2.getError()?.let(::addAll)
        v3.getError()?.let(::addAll)
    }
    return if (errors.isEmpty()) Ok(transform(v1.get()!!, v2.get()!!, v3.get()!!)) else Err(errors)
}

inline fun <E, T1, T2, T3, T4, R> accumulate(
    v1: Validated<E, T1>,
    v2: Validated<E, T2>,
    v3: Validated<E, T3>,
    v4: Validated<E, T4>,
    transform: (T1, T2, T3, T4) -> R,
): Validated<E, R> {
    val errors = buildList {
        v1.getError()?.let(::addAll)
        v2.getError()?.let(::addAll)
        v3.getError()?.let(::addAll)
        v4.getError()?.let(::addAll)
    }
    return if (errors.isEmpty()) {
        Ok(transform(v1.get()!!, v2.get()!!, v3.get()!!, v4.get()!!))
    } else {
        Err(errors)
    }
}

inline fun <E, T1, T2, T3, T4, T5, R> accumulate(
    v1: Validated<E, T1>,
    v2: Validated<E, T2>,
    v3: Validated<E, T3>,
    v4: Validated<E, T4>,
    v5: Validated<E, T5>,
    transform: (T1, T2, T3, T4, T5) -> R,
): Validated<E, R> {
    val errors = buildList {
        v1.getError()?.let(::addAll)
        v2.getError()?.let(::addAll)
        v3.getError()?.let(::addAll)
        v4.getError()?.let(::addAll)
        v5.getError()?.let(::addAll)
    }
    return if (errors.isEmpty()) {
        Ok(transform(v1.get()!!, v2.get()!!, v3.get()!!, v4.get()!!, v5.get()!!))
    } else {
        Err(errors)
    }
}

inline fun <E, T1, T2, T3, T4, T5, T6, R> accumulate(
    v1: Validated<E, T1>,
    v2: Validated<E, T2>,
    v3: Validated<E, T3>,
    v4: Validated<E, T4>,
    v5: Validated<E, T5>,
    v6: Validated<E, T6>,
    transform: (T1, T2, T3, T4, T5, T6) -> R,
): Validated<E, R> {
    val errors = buildList {
        v1.getError()?.let(::addAll)
        v2.getError()?.let(::addAll)
        v3.getError()?.let(::addAll)
        v4.getError()?.let(::addAll)
        v5.getError()?.let(::addAll)
        v6.getError()?.let(::addAll)
    }
    return if (errors.isEmpty()) {
        Ok(transform(v1.get()!!, v2.get()!!, v3.get()!!, v4.get()!!, v5.get()!!, v6.get()!!))
    } else {
        Err(errors)
    }
}

inline fun <E, T1, T2, T3, T4, T5, T6, T7, R> accumulate(
    v1: Validated<E, T1>,
    v2: Validated<E, T2>,
    v3: Validated<E, T3>,
    v4: Validated<E, T4>,
    v5: Validated<E, T5>,
    v6: Validated<E, T6>,
    v7: Validated<E, T7>,
    transform: (T1, T2, T3, T4, T5, T6, T7) -> R,
): Validated<E, R> {
    val errors = buildList {
        v1.getError()?.let(::addAll)
        v2.getError()?.let(::addAll)
        v3.getError()?.let(::addAll)
        v4.getError()?.let(::addAll)
        v5.getError()?.let(::addAll)
        v6.getError()?.let(::addAll)
        v7.getError()?.let(::addAll)
    }
    return if (errors.isEmpty()) {
        Ok(
            transform(
                v1.get()!!, v2.get()!!, v3.get()!!, v4.get()!!,
                v5.get()!!, v6.get()!!, v7.get()!!,
            ),
        )
    } else {
        Err(errors)
    }
}

inline fun <E, T1, T2, T3, T4, T5, T6, T7, T8, R> accumulate(
    v1: Validated<E, T1>,
    v2: Validated<E, T2>,
    v3: Validated<E, T3>,
    v4: Validated<E, T4>,
    v5: Validated<E, T5>,
    v6: Validated<E, T6>,
    v7: Validated<E, T7>,
    v8: Validated<E, T8>,
    transform: (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
): Validated<E, R> {
    val errors = buildList {
        v1.getError()?.let(::addAll)
        v2.getError()?.let(::addAll)
        v3.getError()?.let(::addAll)
        v4.getError()?.let(::addAll)
        v5.getError()?.let(::addAll)
        v6.getError()?.let(::addAll)
        v7.getError()?.let(::addAll)
        v8.getError()?.let(::addAll)
    }
    return if (errors.isEmpty()) {
        Ok(
            transform(
                v1.get()!!, v2.get()!!, v3.get()!!, v4.get()!!,
                v5.get()!!, v6.get()!!, v7.get()!!, v8.get()!!,
            ),
        )
    } else {
        Err(errors)
    }
}

// endregion

/**
 * Validates each element of the collection and **accumulates** the errors of every failing
 * element. Returns `Ok(List<B>)` only if all elements are valid.
 */
inline fun <E, A, B> Iterable<A>.validateEach(validate: (A) -> Validated<E, B>): Validated<E, List<B>> {
    val values = mutableListOf<B>()
    val errors = mutableListOf<E>()
    for (item in this) {
        validate(item).fold(
            success = { values.add(it) },
            failure = { errors.addAll(it) },
        )
    }
    return if (errors.isEmpty()) Ok(values) else Err(errors)
}

/**
 * Folds a list of already-computed results into a single result, **accumulating** every error.
 * Returns `Ok(List<A>)` only if all results are `Ok`.
 */
fun <E, A> Iterable<Validated<E, A>>.combineAll(): Validated<E, List<A>> {
    val values = mutableListOf<A>()
    val errors = mutableListOf<E>()
    for (result in this) {
        result.fold(
            success = { values.add(it) },
            failure = { errors.addAll(it) },
        )
    }
    return if (errors.isEmpty()) Ok(values) else Err(errors)
}

/**
 * Imperative accumulating builder. Checks record their errors as they run; [build] produces an
 * `Ok` only if no error was collected — so the value can be constructed knowing every check
 * passed.
 */
class Accumulator<E> {
    private val errors = mutableListOf<E>()

    /** Records [error] if [condition] is false. */
    fun check(condition: Boolean, error: E) {
        if (!condition) errors.add(error)
    }

    /** Adds an error. */
    fun addError(error: E) {
        errors.add(error)
    }

    /** Adds several errors. */
    fun addErrors(errors: List<E>) {
        this.errors.addAll(errors)
    }

    /** Folds the errors of a nested [Validated] into the accumulator (reuses an existing validator). */
    fun addErrorsOf(result: Validated<E, *>) {
        result.getError()?.let { errors.addAll(it) }
    }

    /** Produces `Ok(value())` if no error was collected, otherwise `Err` of the collected errors. */
    fun <A> build(value: () -> A): Validated<E, A> =
        if (errors.isEmpty()) Ok(value()) else Err(errors.toList())
}

/** Opens an [Accumulator] to accumulate errors imperatively. */
inline fun <E, A> validated(block: Accumulator<E>.() -> Validated<E, A>): Validated<E, A> =
    Accumulator<E>().block()
