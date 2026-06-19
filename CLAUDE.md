# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- Build: `./gradlew build`
- Run tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "validation.AccumulateTest"`
- Run a single test method: `./gradlew test --tests "validation.AccumulateTest.accumulateConcatenatesAllErrors"`
- Run all example tests: `./gradlew test --tests "validation.examples.*"`
- Run the demo `main`: `./gradlew run` (entry point `validation.examples.DemoKt`)

Kotlin 2.2.0, JVM toolchain 21, JUnit 5 (`useJUnitPlatform`), kotlin-result 2.3.1.

## Architecture

Worked examples of **error-accumulating validation** (the applicative validation pattern, not
fail-fast), built on [michaelbull/kotlin-result](https://github.com/michaelbull/kotlin-result).
The carrier type is kotlin-result's `Result<V, E>`; this repo adds the thin **accumulation
layer** that kotlin-result lacks, plus four worked examples. The helper layer lives directly
under `src/main/kotlin/validation/`; worked examples live in `src/main/kotlin/validation/examples/`
and must not leak into the `validation` package.

Helper layer — `validation/Accumulate.kt` (one file):

- `typealias Validated<E, A> = Result<A, List<E>>` — the carrier. **Order is `Result<VALUE,
  ERROR>`** (value first), and the error branch holds a `List<E>` to accumulate.
- `valid(value)` (= `Ok`) / `invalid(error)` (= `Err(listOf(error))`) constructors.
- `accumulate(v1, …, vN, transform)` — combine independent results, **arity 2–8**, concatenating
  every error. Covers `Person.validate`'s 6 fields (above kotlin-result's `zipOrAccumulate` cap
  of 5).
- `Iterable<A>.validateEach { … }` — validate each element, accumulate per-element errors.
- `Iterable<Validated<E, A>>.combineAll()` — fold a list of results, accumulating errors.
- `Accumulator<E>` + `validated { }` — the imperative accumulating builder (`check`,
  `addError`/`addErrors`, `addErrorsOf`, `build { }`).

Examples (`validation/examples/`): `Person.kt`, `Order.kt`, `MoneyTransfer.kt`,
`Registration.kt`, and `Demo.kt` (the `main` that runs them all). They share a small domain
vocabulary within the package (e.g. `CountryCode` from `Person.kt` is reused by `Order.kt`).

Key points:

- **Why a helper layer at all.** kotlin-result is fail-fast by design. Its only accumulating
  combinator, `zipOrAccumulate`, is capped at **5 arguments**, there is **no accumulation over
  collections**, and `binding { }` is fail-fast. `Accumulate.kt` supplies exactly the missing
  pieces (`accumulate` arity 2–8, `validateEach`, `combineAll`, `validated { }`) on top of
  `Result<A, List<E>>`.
- **Accumulation is the central design point.** Combining failures concatenates their error
  lists rather than short-circuiting — in `accumulate` (independent fields), `validateEach`
  (each element of a collection), and `combineAll` (a list of results). Use these to validate
  independent fields/elements and collect every error in one pass.
- **Reuse kotlin-result for everything fail-fast / non-accumulating.** `andThen` (dependent
  path — short-circuits), `recover`/`getOr`/`getOrElse` (recovery), `map`, `mapError`, `fold`,
  `get`/`getError` come straight from kotlin-result. Do **not** reimplement them.
- In kotlin-result 2.x `Result` is an inline value class — `Ok`/`Err` are factory **functions**,
  not subtypes. There is no `is Ok` / `is Err` and no safe direct `.value`/`.error`; use
  `get()` / `getError()` / `fold` to inspect a result.
- **`validated { }` vs `accumulate`.** `accumulate`'s `transform` receives the already-parsed
  values, so it shines for raw input that must be transformed into domain types. The builder's
  `check(condition, error)` only tests a boolean, so it fits invariant checks on already-typed
  values; `addErrorsOf(result)` folds a nested validator's errors in, keeping a rule in one place.

## Conventions

- The examples show the intended styles, all error-accumulating:
  - **Field-combine (preferred for raw input):** `Person.kt`/`Order.kt` — an `Unvalidated…`
    type (primitive fields) → private per-field validators returning `Validated<…Error, FieldType>`
    → assembled with `accumulate(...)`. Collection fields use `validateEach` so every bad element
    is reported (see `validateNationalities` in `Person.kt`, `validateLines` in `Order.kt`).
  - **Builder:** `Person.create(...)` / `RegistrationForm.validate()` use
    `validated { check(...); …; build { } }`. `Person.create` reuses the same per-field
    validators via `addErrorsOf()` — a rule is defined in exactly one place (no duplicated limits).
  - **Dependent step:** `MoneyTransfer.kt` accumulates format errors with `accumulate`, then uses
    `andThen` (from kotlin-result) for the balance check that needs the parsed amount.
- Error types are sealed hierarchies of typed cases that carry the offending data
  (e.g. `FirstNameTooLong(length)`, `InvalidCountryCode(value)`), never strings; singleton
  cases are `data object`.
- Domain primitives use `@JvmInline value class` (e.g. `BirthDate`, `CountryCode`, `Money`, `Iban`).
- Tests assert on structural equality of `Ok(...)` / `Err(listOf(...))` and explicitly verify
  the accumulation case; when error *order* is unstable across fields, compare
  `result.getError().orEmpty().toSet()`. Validators must be deterministic — `validateBirth`
  checks against a fixed `EARLIEST_BIRTH`, not `now()`.

## Notes

- Doc comments on the `Accumulate.kt` helper layer (the library core) are in English; the
  `examples/` files have French comments — match the language of the file you are editing.
