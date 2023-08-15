package com.centralbank.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.issue.addIssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalArgumentException

@StartableByRPC
class FiatCurrencyIssueFlow(
    private val currency: String,
    private val amount: Long,
    private val recipient: Party
) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        /* Create an instance of the fiat currency token */
        val token = FiatCurrency.Companion.getInstance(currency)

        when (currency) {
            "IDR" -> requireThat { "Must be issued by Bank Indonesia".using(ourIdentity == serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=INDOIDJA, L=Jakarta, C=ID"))) }
            "SGD" -> requireThat { "Must be issued by Monetary Authority of Singapore".using(ourIdentity == serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=MASGSGSG, L=Singapore, C=SG"))) }
            "INR" -> requireThat { "Must be issued by Reserve Bank of India".using(ourIdentity == serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=RBISINBB, L=Mumbai, C=IN"))) }
        }


        /* Create an instance of IssuedTokenType for the fiat currency */
        val issuedTokenType = token issuedBy ourIdentity

        /* Create an instance of FungibleToken for the fiat currency to be issued */
        val fungibleToken = FungibleToken(amount(amount,issuedTokenType),recipient)

        val stx = subFlow(IssueTokens(listOf(fungibleToken), listOf(recipient)))
        return "Issued ${fungibleToken.amount.toDecimal()} $currency token(s) to ${recipient.name.organisation}"
    }
}
