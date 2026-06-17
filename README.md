# kotlin-validation

A tiny Kotlin library for **error-accumulating validation** — the applicative validation
pattern, *not* fail-fast. Validating a form or request collects **every** error in a single
pass instead of stopping at the first failure.

The API is deliberately **validation-oriented, not FP/category-theory jargon**: you will find
`combine`, `validateEach`, and `andThen` — never `zip`, `ap`, `mapN`, `sequence`, `traverse`,
or `flatMap`.

## Why

Fail-fast validation (e.g. throwing on the first bad field, or short-circuiting `Result`
chains) reports one problem at a time. A user fixes it, resubmits, and discovers the next
one. Error-accumulating validation runs all independent checks and reports them together:

```text
Invalid(errors = [
  FirstNameTooLong(length = 80),
  InvalidCountryCode(value = "XX"),
  NationalitiesEmpty,
])
```

## Core concept

`Validation<E, A>` is a sealed interface with exactly two cases:

```kotlin
data class Valid<out A>(val value: A) : Validation<Nothing, A>
data class Invalid<out E>(val errors: List<E>) : Validation<E, Nothing>
```

Construct them with `valid(...)` / `invalid(...)`. Combining two `Invalid`s **concatenates
their error lists** rather than short-circuiting — that is the whole point.

Error types are sealed hierarchies of typed cases that carry the offending data
(`FirstNameTooLong(length)`, `InvalidCountryCode(value)`), never strings. Singleton cases are
`data object`.

## Quick start

Define an unvalidated input, write one validator per field returning
`Validation<…Error, FieldType>`, then assemble them with `combine`:

```kotlin
data class UnvalidatedOrder(
    val lines: List<UnvalidatedOrderLine>,
    val shipping: UnvalidatedShippingAddress,
    val currency: String,
)

data class Order(
    val lines: List<OrderLine>,
    val shipping: ShippingAddress,
    val currency: CurrencyCode,
)

fun UnvalidatedOrder.validate(): Validation<OrderError, Order> = combine(
    validateLines(lines),
    validateAddress(shipping),
    validateCurrency(currency),
) { lines, shipping, currency -> Order(lines, shipping, currency) }
```

If any field is invalid, the result is an `Invalid` holding the accumulated errors from
*all* failing fields; otherwise a `Valid<Order>`.

## The two styles

**Same outcome.** Both styles produce a `Validation<E, A>` that accumulates *every* error
in a single pass — neither is fail-fast (that is `andThen`). `Person.kt` validates an
equivalent `Person` both ways: `UnvalidatedPerson.validate()` via `combine`, and
`Person.create(...)` via the DSL.

**Dual mechanism.** They express the same intent differently:

| | Field-combine | DSL `validation { }` |
| --- | --- | --- |
| Style | expression / functional | imperative blocks |
| Accumulation | immutable `Invalid(errors1 + errors2)` in `combine` | mutable list in `ValidationScope` |
| Value flow | validated values are carried into `transform` (already typed) | values are dropped; the object is rebuilt in `build { }` |
| Construction guarantee | `Valid` only exists if every field is `Valid` (enforced by types) | `build` runs only if no error was recorded (enforced at runtime) |

The key practical consequence is the *value flow* row: `combine`'s `transform` receives the
**parsed** values (e.g. `String` -> `CivilStatus`), so field-combine shines when validation
**transforms** raw input. The DSL's `check(condition, error)` only tests a boolean and hands
nothing back, so it fits **invariant checks on already-typed values**.

### Field-combine (preferred for raw input)

Validate independent fields and merge them with `combine`. Collection fields use
`validateEach` so every bad element is reported, not just the first:

```kotlin
private fun validateLines(lines: List<UnvalidatedOrderLine>): Validation<OrderError, List<OrderLine>> =
    if (lines.isEmpty()) invalid(OrderError.EmptyCart)
    else lines.validateEach(::validateLine)

private fun validateLine(line: UnvalidatedOrderLine): Validation<OrderError, OrderLine> = combine(
    validateSku(line),
    validateQuantity(line),
    validatePrice(line),
) { sku, quantity, price -> OrderLine(sku, quantity, price) }
```

`combine` comes as a two-validation extension (`a.combine(b) { … }`) and as top-level
overloads for **arity 3–8**. Use `combineAll()` to fold a `List<Validation<E, A>>` into a
`Validation<E, List<A>>`.

### DSL

`validation { … }` opens a `ValidationScope<E>` where checks accumulate imperatively and
`build { }` produces `Valid` only if no error was collected:

```kotlin
fun RegistrationForm.validate(): Validation<RegistrationError, Account> = validation {
    addErrorsOf(validateEmail(email))                  // fold a nested Validation's errors in

    check(password.length >= MIN_PASSWORD_LENGTH, RegistrationError.PasswordTooShort(password.length))
    check(isStrong(password), RegistrationError.PasswordTooWeak)
    check(password == passwordConfirmation, RegistrationError.PasswordMismatch)
    check(age >= MINIMUM_AGE, RegistrationError.Underage(age))
    check(termsAccepted, RegistrationError.TermsNotAccepted)

    build { Account(Email(email), age) }               // runs only if no errors were collected
}
```

`addErrorsOf(...)` reuses an existing validator, so a rule lives in exactly one place — no
duplicated limits between the `combine` and DSL paths.

### When to use which

- **Field-combine** — raw `Unvalidated…` input where each field must be
  **parsed/transformed** into a domain type, with one reusable validator per field. Limited
  to arity 3–8 (plus the two-validation extension).
- **DSL** — values are already typed and you mostly **check invariants**, you need
  conditional or cross-field logic, or you want to reuse validators via `addErrorsOf(...)`
  alongside ad-hoc rules (as in `Person.create`). No arity cap, so it also scales to many
  fields.
- **Neither** — when a step *depends* on the result of a previous one, use `andThen` (see
  [Dependent steps](#dependent-steps)).

## Dependent steps

`combine` is for *independent* checks. When a step needs the result of a previous one, use
`andThen` (the fail-fast, dependent path): accumulate the format errors first, then chain the
dependent check.

```kotlin
fun TransferRequest.validate(balanceCents: Long): Validation<TransferError, Transfer> =
    validateAmount(amountCents).combine(validateIban(targetIban)) { amount, target ->
        Transfer(amount, target)
    }.andThen { transfer ->
        if (transfer.amount.cents <= balanceCents) valid(transfer)
        else invalid(TransferError.InsufficientFunds(balanceCents, transfer.amount.cents))
    }
```

Prefer `combine` whenever checks are independent so all errors surface; reach for `andThen`
only when a step genuinely depends on the previous result.

## API reference

### Construct & consume — `Validation.kt`

| Function | Signature |
| --- | --- |
| `valid` | `fun <A> valid(value: A): Validation<Nothing, A>` |
| `invalid` | `fun <E> invalid(error: E): Validation<E, Nothing>` / `invalid(errors: List<E>)` |
| `toValidation` | `fun <E, A> A?.toValidation(error: () -> E): Validation<E, A>` |
| `fold` | `fun <R> fold(ifInvalid: (List<E>) -> R, ifValid: (A) -> R): R` |
| `getOrNull` | `fun getOrNull(): A?` |
| `errorsOrEmpty` | `fun errorsOrEmpty(): List<E>` |
| `onValid` / `onInvalid` | `fun onValid(action: (A) -> Unit): Validation<E, A>` (and `onInvalid`) |
| `isValid` / `isInvalid` | `val isValid: Boolean`, `val isInvalid: Boolean` |

### Transform — `ValidationTransforms.kt`

| Function | Signature |
| --- | --- |
| `map` | `fun <E, A, B> Validation<E, A>.map(f: (A) -> B): Validation<E, B>` |
| `mapError` | `fun <E1, E2, A> Validation<E1, A>.mapError(f: (E1) -> E2): Validation<E2, A>` |
| `getOrElse` | `fun <E, A> Validation<E, A>.getOrElse(default: (List<E>) -> A): A` |
| `toResult` | `fun <E, A> Validation<E, A>.toResult(onErrors: (List<E>) -> Throwable): Result<A>` |

### Combine / accumulate — `ValidationCombine.kt`

| Function | Signature |
| --- | --- |
| `combine` (extension) | `Validation<E, A>.combine(other: Validation<E, B>, transform: (A, B) -> R): Validation<E, R>` |
| `combine` (top-level) | `combine(v1, …, vN, transform)` — **arity 3–8** |
| `combineAll` | `fun <E, A> Iterable<Validation<E, A>>.combineAll(): Validation<E, List<A>>` |
| `validateEach` | `fun <E, A, B> Iterable<A>.validateEach(validate: (A) -> Validation<E, B>): Validation<E, List<B>>` |

### Sequence — `ValidationSequencing.kt`

| Function | Signature |
| --- | --- |
| `andThen` | `fun <E, A, B> Validation<E, A>.andThen(next: (A) -> Validation<E, B>): Validation<E, B>` (fail-fast / dependent) |
| `orElse` | `fun <E, A> Validation<E, A>.orElse(recover: (List<E>) -> Validation<E, A>): Validation<E, A>` (recovery) |

### DSL — `ValidationDsl.kt`

```kotlin
fun <E, A> validation(block: ValidationScope<E>.() -> Validation<E, A>): Validation<E, A>

class ValidationScope<E> {
    fun check(condition: Boolean, error: E)
    fun ensure(condition: Boolean, error: E)
    fun addError(error: E)
    fun addErrors(errors: List<E>)
    fun addErrorsOf(validation: Validation<E, *>)
    fun <A> build(value: () -> A): Validation<E, A>
}
```

### Reusable rules — `Validator.kt`

```kotlin
typealias Validator<E, A> = (A) -> Validation<E, A>

fun <E, A> validator(error: (A) -> E, predicate: (A) -> Boolean): Validator<E, A>
infix fun <E, A> Validator<E, A>.and(other: Validator<E, A>): Validator<E, A>
fun <E, A> allOf(vararg validators: Validator<E, A>): Validator<E, A>
```

## Examples

Worked examples live in `src/main/kotlin/validation/examples/`:

- **`Person.kt`** — field-combine and the `validation { }` DSL, with `validateEach` over a list of nationalities.
- **`Order.kt`** — nested `combine` and `validateEach` over order lines.
- **`MoneyTransfer.kt`** — `combine` for format checks, then `andThen` for the balance check.
- **`Registration.kt`** — full DSL style.
- **`Demo.kt`** — a `main` that runs them all.

## Build & run

```bash
./gradlew build                                            # compile + test
./gradlew test                                             # run all tests
./gradlew test --tests "validation.ValidationTest"         # a single test class
./gradlew test --tests "validation.examples.*"             # all example tests
./gradlew run                                              # run the demo (validation.examples.DemoKt)
```

Stack: **Kotlin 2.2.0**, **JVM toolchain 21**, **JUnit 5** (`useJUnitPlatform`).

## Design notes

- **Accumulation is the central design point** — `combine` concatenates error lists; use it
  to validate independent fields and collect every error in one pass.
- **`getOrElse` is an extension, not a member.** A member would erase the value type to
  `Nothing` on `Invalid` and throw `ClassCastException` when the fallback is used. Value-typed
  accessors stay as extensions for this reason.
- **Validators are deterministic** — e.g. birth-date checks compare against a fixed earliest
  date, never `now()`, so tests are stable.
- Domain primitives use `@JvmInline value class` (`BirthDate`, `CountryCode`, `Money`, `Iban`).
