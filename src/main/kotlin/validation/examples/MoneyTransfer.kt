package validation.examples

import com.github.michaelbull.result.andThen
import validation.Validated
import validation.accumulate
import validation.invalid
import validation.valid

/**
 * Exemple : validation d'un virement bancaire.
 *
 * Illustre la combinaison des deux modes :
 *  - les contrôles de *format* indépendants (montant, IBAN) sont accumulés via [accumulate] ;
 *  - puis `andThen` (fourni par kotlin-result) enchaîne un contrôle *dépendant* (solde suffisant)
 *    qui a besoin du montant déjà validé. Ce contrôle ne s'exécute que si le format est correct —
 *    inutile de vérifier le solde tant qu'on ne connaît pas un montant valide.
 */

/** Montant en centimes (évite les flottants). */
@JvmInline
value class Money(val cents: Long)

/** Numéro de compte IBAN validé. */
@JvmInline
value class Iban(val value: String)

/** Virement validé, conforme par construction. */
data class Transfer(val amount: Money, val target: Iban)

/** Demande de virement brute (montant et IBAN non validés). */
data class TransferRequest(val amountCents: Long, val targetIban: String)

/** Hiérarchie d'erreurs pour la validation d'un virement. */
sealed interface TransferError {
    data class NonPositiveAmount(val cents: Long) : TransferError
    data class MalformedIban(val value: String) : TransferError
    data class InsufficientFunds(val balanceCents: Long, val amountCents: Long) : TransferError
}

private fun validateAmount(cents: Long): Validated<TransferError, Money> =
    if (cents > 0) valid(Money(cents)) else invalid(TransferError.NonPositiveAmount(cents))

private fun validateIban(value: String): Validated<TransferError, Iban> {
    val normalized = value.replace(" ", "").uppercase()
    val wellFormed = normalized.length in 15..34 &&
        normalized.take(2).all(Char::isLetter) &&
        normalized.drop(2).all(Char::isLetterOrDigit)
    return if (wellFormed) valid(Iban(normalized)) else invalid(TransferError.MalformedIban(value))
}

/**
 * Valide une demande de virement débitée d'un compte au solde [balanceCents].
 *
 * Format (montant + IBAN) accumulé d'abord, puis vérification dépendante du solde.
 */
fun TransferRequest.validate(balanceCents: Long): Validated<TransferError, Transfer> =
    accumulate(validateAmount(amountCents), validateIban(targetIban)) { amount, target -> Transfer(amount, target) }
        .andThen { transfer ->
            if (transfer.amount.cents <= balanceCents) valid(transfer)
            else invalid(TransferError.InsufficientFunds(balanceCents, transfer.amount.cents))
        }
