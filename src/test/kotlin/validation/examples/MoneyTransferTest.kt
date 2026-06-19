package validation.examples

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MoneyTransferTest {

    private val balance = 100_00L // 100.00 in cents

    @Test
    fun validTransferWithinBalance() {
        val result = TransferRequest(amountCents = 50_00, targetIban = "FR76 3000 6000 0112 3456 7890 189")
            .validate(balance)
        assertEquals(Ok(Transfer(Money(50_00), Iban("FR7630006000011234567890189"))), result)
    }

    @Test
    fun accumulatesFormatErrorsBeforeBalanceCheck() {
        // both the amount and the IBAN are malformed -> both reported in one pass,
        // and the balance check (andThen) never runs
        val result = TransferRequest(amountCents = -1, targetIban = "oops").validate(balance)
        assertEquals(
            setOf(TransferError.NonPositiveAmount(-1), TransferError.MalformedIban("oops")),
            result.getError().orEmpty().toSet(),
        )
    }

    @Test
    fun dependentBalanceCheckRunsOnlyAfterFormatPasses() {
        // format is valid, so the dependent step runs and rejects the overdraft
        val result = TransferRequest(amountCents = 200_00, targetIban = "FR7630006000011234567890189")
            .validate(balance)
        assertEquals(Err(listOf(TransferError.InsufficientFunds(balance, 200_00))), result)
    }
}
