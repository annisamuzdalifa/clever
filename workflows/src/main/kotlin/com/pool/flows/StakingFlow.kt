package com.pool.flows

import co.paralleluniverse.fibers.Suspendable
import com.pool.LPToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.selection.memory.selector.LocalTokenSelector
import com.r3.corda.lib.tokens.workflows.flows.issue.addIssueTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.secureRandomBytes
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.lang.IllegalArgumentException
import java.util.*

@InitiatingFlow
@StartableByRPC
/**
 * Initiated by liquidity provider to staked their asset. Expect LPTokens in returns
 */
class StakingFlow(
    private val currency: String,
    val amount : Long
) : FlowLogic<String>() {
    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=Jakarta,C=ID"))
        val lPool = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager,L=Jakarta,C=ID"))!!

        // Check balance
        val balance = serviceHub.vaultService.tokenBalance(TokenType(currency, 2))
        if (balance.quantity.times(balance.displayTokenSize.toDouble()) < amount.toDouble()) {
            IllegalArgumentException("Insufficient token balance to be staked")
        }

        val token = FiatCurrency.Companion.getInstance(currency)
        val currencyToken = amount(amount, token)


        // Add Fiat Currency move dari LP -> LiquidityPoolManager
        val tb = TransactionBuilder(notary)
        val inputsAndOutputs = LocalTokenSelector(serviceHub).generateMove(
            lockId = tb.lockId,
            partiesAndAmounts = listOf(Pair(lPool, currencyToken)),
            changeHolder = ourIdentity,
            holdingKey = ourIdentity.owningKey
        )

        addMoveTokens(tb, inputsAndOutputs.first, inputsAndOutputs.second)

        // Get LPToken from liquidity pool manager
        val lpoolSession = initiateFlow(lPool)
        lpoolSession.send(inputsAndOutputs.second.first())
        val lpTokenReceived: FungibleToken = lpoolSession.receive<FungibleToken>().unwrap { it->it }
        addIssueTokens(tb, lpTokenReceived)

        tb.verify(serviceHub)

        val initialSignedTrx = serviceHub.signInitialTransaction(tb)

        val stx = subFlow(CollectSignaturesFlow(initialSignedTrx, listOf(lpoolSession)))

        val ftx = subFlow(FinalityFlow(stx, listOf(lpoolSession)))

        subFlow(UpdateDistributionListFlow(ftx))

        return "Successfully stake $currency $amount to Pool"

    }
}

@InitiatedBy(StakingFlow::class)
class StakingFlowResponder(
    val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call() : SignedTransaction {

        // ADD CHECKING token type
        val token = otherSession.receive<FungibleToken>().unwrap { it } // receive token for LPManager
        val currency = token.amount.token.tokenIdentifier
        val lpToken = FungibleTokenBuilder()
            .ofTokenType(LPToken(currency = Currency.getInstance(currency)))
            .withAmount(token.amount.quantity/100)
            .issuedBy(ourIdentity) // LPM
            .heldBy(otherSession.counterparty)
            .buildFungibleToken()

        // ADD broadcast token to All
        otherSession.send(lpToken)

        //signing
        subFlow(object : SignTransactionFlow(otherSession) {
            
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) { // Custom Logic to validate transaction.
            }
        })
        return subFlow(ReceiveFinalityFlow(otherSideSession = otherSession,
            statesToRecord = StatesToRecord.ALL_VISIBLE))
    }

}