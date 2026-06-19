# kotlin-validation

Worked examples of **error-accumulating validation** — the applicative validation pattern,
*not* fail-fast — built on top of [michaelbull/kotlin-result](https://github.com/michaelbull/kotlin-result).
Validating a form or request collects **every** error in a single pass instead of stopping at
the first failure.

kotlin-result is fail-fast by design (`andThen`, `binding`). It does ship a `zipOrAccumulate`,
but it is capped at **5 arguments**, has **no accumulation over collections**, and no
accumulating builder. This repo adds the missing **accumulation layer** as a thin set of
helpers in [`Accumulate.kt`](src/main/kotlin/validation/Accumulate.kt) and shows four worked
examples on top of it.

## Why

Fail-fast validation (throwing on the first bad field, or short-circuiting a `Result` chain)
reports one problem at a time. A user fixes it, resubmits, and discovers the next one.
Error-accumulating validation runs all independent checks and reports them together:

```text
Err([
  FirstNameTooLong(length = 51),
  InvalidCountryCode(value = "FRA"),
  NationalitiesEmpty,
])
```

## Core concept

The carrier type is kotlin-result's `Result<V, E>` — note the order **value first, error
second** (the opposite of an `Either`/`Validation<E, A>` signature). The error branch carries a
**`List<E>`** so multiple errors accumulate rather than short-circuit:

```kotlin
typealias Validated<E, A> = Result<A, List<E>>
```

Construct results with `valid(value)` (= `Ok`) and `invalid(error)` (= `Err(listOf(error))`).
Combining failures **concatenates their error lists** — that is the whole point.

Error types are sealed hierarchies of typed cases that carry the offending data
(`FirstNameTooLong(length)`, `InvalidCountryCode(value)`), never strings. Singleton cases are
`data object`.

## Quick start

Define an unvalidated input, write one validator per field returning `Validated<…Error,
FieldType>`, then assemble them with `accumulate`:

```kotlin
fun UnvalidatedOrder.validate(): Validated<OrderError, Order> = accumulate(
    validateLines(lines),
    validateAddress(shipping),
    validateCurrency(currency),
) { lines, shipping, currency -> Order(lines, shipping, currency) }
```

If any field is invalid, the result is an `Err` holding the accumulated errors from *all*
failing fields; otherwise `Ok(Order(...))`.

## The accumulation layer (`Accumulate.kt`)

| Helper | What it does | Replaces / why |
| --- | --- | --- |
| `accumulate(v1, …, vN, transform)` | Combine **2–8** independent results, concatenating every error | kotlin-result's `zipOrAccumulate` caps at 5 args |
| `Iterable<A>.validateEach { … }` | Validate each element, accumulate per-element errors | no collection accumulation in kotlin-result |
| `Iterable<Validated<E, A>>.combineAll()` | Fold a list of results, accumulating errors | `combine` in kotlin-result is fail-fast |
| `validated { check(…); addErrorsOf(…); build { … } }` | Imperative accumulating builder | `binding { }` in kotlin-result is fail-fast |

Everything **fail-fast / non-accumulating** is reused straight from kotlin-result: `andThen`,
`recover`/`getOr`/`getOrElse`, `map`, `mapError`, `fold`, `get`/`getError`.

### Field-combine (preferred for raw input)

Validate independent fields and merge them with `accumulate`. Collection fields use
`validateEach` so every bad element is reported, not just the first:

```kotlin
private fun validateLines(lines: List<UnvalidatedOrderLine>): Validated<OrderError, List<OrderLine>> =
    if (lines.isEmpty()) invalid(OrderError.EmptyCart)
    else lines.validateEach(::validateLine)

private fun validateLine(line: UnvalidatedOrderLine): Validated<OrderError, OrderLine> = accumulate(
    validateSku(line),
    validateQuantity(line),
    validatePrice(line),
) { sku, quantity, price -> OrderLine(sku, quantity, price) }
```

`accumulate` is overloaded for **arity 2–8** — so `Person.validate` can combine its 6 fields in
one call, above kotlin-result's native `zipOrAccumulate` limit of 5.

### Builder (`validated { }`)

`validated { … }` opens an `Accumulator<E>` where checks accumulate imperatively and
`build { }` produces `Ok` only if no error was collected:

```kotlin
fun RegistrationForm.validate(): Validated<RegistrationError, Account> = validated {
    addErrorsOf(validateEmail(email))                  // fold a nested result's errors in

    check(password.length >= MIN_PASSWORD_LENGTH, RegistrationError.PasswordTooShort(password.length))
    check(isStrong(password), RegistrationError.PasswordTooWeak)
    check(password == passwordConfirmation, RegistrationError.PasswordMismatch)
    check(age >= MINIMUM_AGE, RegistrationError.Underage(age))
    check(termsAccepted, RegistrationError.TermsNotAccepted)

    build { Account(Email(email), age) }               // runs only if no errors were collected
}
```

`addErrorsOf(...)` reuses an existing field validator, so a rule lives in exactly one place —
no duplicated limits between the `accumulate` and builder paths (see `Person.create`).

### When to use which

- **Field-combine (`accumulate`)** — raw `Unvalidated…` input where each field is
  **parsed/transformed** into a domain type; `transform` receives the already-typed values.
- **Builder (`validated`)** — values are already typed and you mostly **check invariants**, need
  conditional/cross-field logic, or want to reuse validators via `addErrorsOf(...)` alongside
  ad-hoc rules.
- **Neither** — when a step *depends* on the result of a previous one, use `andThen` (below).

## Dependent steps

`accumulate` is for *independent* checks. When a step needs the result of a previous one, use
kotlin-result's `andThen` (the fail-fast, dependent path): accumulate the format errors first,
then chain the dependent check.

```kotlin
fun TransferRequest.validate(balanceCents: Long): Validated<TransferError, Transfer> =
    accumulate(validateAmount(amountCents), validateIban(targetIban)) { amount, target ->
        Transfer(amount, target)
    }.andThen { transfer ->
        if (transfer.amount.cents <= balanceCents) valid(transfer)
        else invalid(TransferError.InsufficientFunds(balanceCents, transfer.amount.cents))
    }
```

Prefer `accumulate` whenever checks are independent so all errors surface; reach for `andThen`
only when a step genuinely depends on the previous result.

## Examples

Worked examples live in `src/main/kotlin/validation/examples/`:

- **`Person.kt`** — field-combine (`accumulate`, arity 6) and the `validated { }` builder, with `validateEach` over a list of nationalities.
- **`Order.kt`** — nested `accumulate` and `validateEach` over order lines.
- **`MoneyTransfer.kt`** — `accumulate` for format checks, then `andThen` for the balance check.
- **`Registration.kt`** — full `validated { }` builder style.
- **`Demo.kt`** — a `main` that runs them all.

## Build & run

```bash
./gradlew build                                            # compile + test
./gradlew test                                             # run all tests
./gradlew test --tests "validation.AccumulateTest"         # the accumulation-layer tests
./gradlew test --tests "validation.examples.*"             # all example tests
./gradlew run                                              # run the demo (validation.examples.DemoKt)
```

Stack: **Kotlin 2.2.0**, **JVM toolchain 21**, **JUnit 5** (`useJUnitPlatform`),
**kotlin-result 2.3.1**.

## Design notes

- **Accumulation is the central design point** — `accumulate`/`validateEach`/`combineAll`
  concatenate error lists; use them to validate independent fields/elements and collect every
  error in one pass.
- **kotlin-result supplies the fail-fast half** — `andThen`, `recover`, `map`, `mapError`,
  `fold`, `getOr` are used directly; this repo only adds what's missing for accumulation.
- **Validators are deterministic** — e.g. birth-date checks compare against a fixed earliest
  date, never `now()`, so tests are stable.
- Domain primitives use `@JvmInline value class` (`BirthDate`, `CountryCode`, `Money`, `Iban`).
