package validation

// --- Dependent / fail-fast sequencing ---
//
// Unlike `combine`, these do NOT accumulate: an `Invalid` short-circuits. Use `andThen`
// only when a step genuinely depends on the previous result (so accumulating is impossible);
// otherwise prefer `combine` so every independent error surfaces.

/**
 * Runs [next] with the value only if valid; an [Invalid] short-circuits unchanged.
 *
 * Use this for *dependent* validations where a later step needs the result of an earlier
 * one. For *independent* fields that should all report their errors, prefer [combine].
 */
inline fun <E, A, B> Validation<E, A>.andThen(next: (A) -> Validation<E, B>): Validation<E, B> = when (this) {
    is Valid -> next(value)
    is Invalid -> this
}

/**
 * Returns this if valid, otherwise the recovery result produced by [recover] from the errors.
 */
inline fun <E, A> Validation<E, A>.orElse(recover: (List<E>) -> Validation<E, @UnsafeVariance A>): Validation<E, A> =
    when (this) {
        is Valid -> this
        is Invalid -> recover(errors)
    }
