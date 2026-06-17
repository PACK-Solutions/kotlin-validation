# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- Build: `./gradlew build`
- Run tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "validation.ValidationTest"`
- Run a single test method: `./gradlew test --tests "validation.ValidationTest.testDSL"`
- Run all example tests: `./gradlew test --tests "validation.examples.*"`
- Run the demo `main`: `./gradlew run` (entry point `validation.examples.DemoKt`)

Kotlin 2.2.0, JVM toolchain 21, JUnit 5 (`useJUnitPlatform`).

## Architecture

A tiny library for **error-accumulating validation** (the applicative validation pattern, not
fail-fast). Naming is deliberately **validation-oriented, not FP/category-theory jargon**
(no `zip`/`ap`/`mapN`/`sequence`/`traverse`/`flatMap`). The core lib lives under
`src/main/kotlin/validation/`; worked examples live in `src/main/kotlin/validation/examples/`
and must not leak into the core package.

Core (`validation/`), split by concern:

- `Validation.kt` — the `Validation<E, A>` sealed type plus consumption helpers (`fold`,
  `getOrNull`, `errorsOrEmpty`, `onValid`/`onInvalid`, `isValid`/`isInvalid`), the
  `valid`/`invalid` constructors, and `toValidation` (lift a nullable).
- `ValidationTransforms.kt` — `map`, `mapError`, `getOrElse`, and `toResult` (interop).
- `ValidationCombine.kt` — the error-accumulating combiners: `combine`, `combineAll`, `validateEach`.
- `ValidationSequencing.kt` — the dependent/fail-fast path: `andThen`, `orElse`.
- `ValidationDsl.kt` — the `validation { }` builder and `ValidationScope`.
- `Validator.kt` — the reusable `Validator<E, A>` rule type and its combinators (`validator`, `and`, `allOf`).

Examples (`validation/examples/`): `Person.kt`, `Order.kt`, `MoneyTransfer.kt`,
`Registration.kt`, and `Demo.kt` (the `main` that runs them all). They share a small domain
vocabulary within the package (e.g. `CountryCode` from `Person.kt` is reused by `Order.kt`).

Key points:

- `Validation<E, A>` is a sealed interface with two cases: `Valid<A>` (a value) and
  `Invalid<E>` (a `List<E>` of accumulated errors). `valid(...)`/`invalid(...)` construct them.
- **Accumulation is the central design point.** Combining two `Invalid`s concatenates their
  error lists rather than short-circuiting. This happens in `combine` — both the
  `Validation<E, A>.combine(other[, transform])` extension (two validations) and the top-level
  `combine(v1, …, vN, transform)` overloads (**arity 3–8**, built on the extension) — and in the
  collection combiners `combineAll` (a list of validations) / `validateEach` (validate each
  element). Use these to validate independent fields/elements and collect every error in one pass.
- **`getOrElse` is an extension, not a member** (in `ValidationTransforms.kt`). A member
  would erase the value type to `Nothing` on `Invalid` and throw `ClassCastException` at
  runtime when the fallback is used. Keep value-typed accessors as extensions for this reason.
- `andThen` is the **fail-fast, dependent** path (an `Invalid` short-circuits) — use it only
  when a step needs the previous result; otherwise prefer `combine` so all errors surface.
  `orElse` recovers from errors; `toResult` converts to `kotlin.Result`.
- **DSL alternative** to `combine`: `validation { ... }` opens a `ValidationScope<E>` where
  `check`/`ensure(condition, error)` and `addError(...)`/`addErrors(...)` accumulate errors imperatively,
  `addErrorsOf(validation)` folds a nested `Validation`'s errors into the scope (reusing an
  existing validator), and `build { value }` produces `Valid` only if no errors were collected —
  so value classes can be constructed in `build` knowing every check passed.

## Conventions

- The examples show the intended styles, all error-accumulating:
  - **Field-combine (preferred for raw input):** `Person.kt`/`Order.kt` — an `Unvalidated…`
    type (primitive fields) → private per-field validators returning `Validation<…Error, FieldType>`
    → assembled with `combine(...)`. Collection fields use `validateEach` so every bad element is
    reported (see `validateNationalities` in `Person.kt`, `validateLines` in `Order.kt`).
  - **DSL:** `Person.create(...)` / `RegistrationForm.validate()` use
    `validation { check(...); …; build { } }`. `Person.create` reuses the same per-field
    validators via `addErrorsOf()` — a rule is defined in exactly one place (no duplicated limits).
  - **Dependent step:** `MoneyTransfer.kt` accumulates format errors with `combine`, then uses
    `andThen` for the balance check that needs the parsed amount.
- Error types are sealed hierarchies of typed cases that carry the offending data
  (e.g. `FirstNameTooLong(length)`, `InvalidCountryCode(value)`), never strings; singleton
  cases are `data object`.
- Domain primitives use `@JvmInline value class` (e.g. `BirthDate`, `CountryCode`, `Money`, `Iban`).
- Tests assert on structural equality of `Valid`/`Invalid` and explicitly verify the
  accumulation case; when error *order* is unstable across fields, compare
  `errorsOrEmpty().toSet()`. Validators must be deterministic — `validateBirth` checks against
  a fixed `EARLIEST_BIRTH`, not `now()`.

## Notes

- Doc comments on the core public API are in English; the `examples/` files have French
  comments — match the language of the file you are editing.
