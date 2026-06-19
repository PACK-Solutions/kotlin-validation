package validation.examples

import java.time.LocalDate

/**
 * Démonstration : pour chaque exemple métier, un cas valide puis un cas invalide dont
 * toutes les erreurs sont accumulées en un seul passage.
 */
fun main() {
    demoPerson()
    demoOrder()
    demoTransfer()
    demoRegistration()
}

private fun demoPerson() {
    println("=== Personne (Unvalidated -> validate -> accumulate) ===")
    println(
        UnvalidatedPerson(
            firstName = "Marie",
            lastName = "Dubois",
            birth = LocalDate.of(1990, 2, 21),
            civilStatus = "MRS",
            socialSecurityNumber = "2 90 02 21 999 999 99",
            nationalities = listOf("FR", "be"),
        ).validate(),
    )
    println(
        UnvalidatedPerson(
            firstName = "A".repeat(51),
            lastName = "B".repeat(61),
            birth = LocalDate.of(1850, 1, 1),
            civilStatus = "X",
            socialSecurityNumber = "",
            nationalities = listOf("FRA", "BE"),
        ).validate(),
    )
}

private fun demoOrder() {
    println("\n=== Commande (validateEach sur les lignes + accumulate) ===")
    val address = UnvalidatedShippingAddress("12 rue des Lilas", "Lyon", "69003", "FR")
    println(
        UnvalidatedOrder(
            lines = listOf(UnvalidatedOrderLine("SKU-1", 2, 1990), UnvalidatedOrderLine("SKU-2", 1, 500)),
            shipping = address,
            currency = "EUR",
        ).validate(),
    )
    println(
        UnvalidatedOrder(
            lines = listOf(UnvalidatedOrderLine("SKU-1", 0, -100), UnvalidatedOrderLine("", 3, 200)),
            shipping = UnvalidatedShippingAddress("", "Lyon", "69003", "FRA"),
            currency = "XXX",
        ).validate(),
    )
}

private fun demoTransfer() {
    println("\n=== Virement (accumulate puis andThen dépendant du solde) ===")
    val balance = 100_00L
    println(TransferRequest(amountCents = 50_00, targetIban = "FR76 3000 6000 0112 3456 7890 189").validate(balance))
    println(TransferRequest(amountCents = -1, targetIban = "oops").validate(balance))
    println(TransferRequest(amountCents = 200_00, targetIban = "FR7630006000011234567890189").validate(balance))
}

private fun demoRegistration() {
    println("\n=== Inscription (builder validated) ===")
    println(
        RegistrationForm(
            email = "alice@example.com",
            password = "s3cret-pass",
            passwordConfirmation = "s3cret-pass",
            age = 30,
            termsAccepted = true,
        ).validate(),
    )
    println(
        RegistrationForm(
            email = "not-an-email",
            password = "short",
            passwordConfirmation = "different",
            age = 15,
            termsAccepted = false,
        ).validate(),
    )
}
