package com.pool.flows

import co.paralleluniverse.fibers.Suspendable
import com.pool.FeeToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.selection.memory.selector.LocalTokenSelector
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.flows.redeem.addTokensToRedeem
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.util.*


@InitiatingFlow
@StartableByRPC
class RedeemFeeFlow(
    private val currency: String,
    private val amount : Long
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=Jakarta,C=ID"))
        val lPool = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager,L=Jakarta,C=ID"))!!
        val tokenType = FeeToken(Currency.getInstance(currency))

        // Check balance
        val balance = serviceHub.vaultService.tokenBalance(tokenType)
        logger.info("has balance $balance")
        if (balance.quantity.times(balance.displayTokenSize.toDouble()) < amount.toDouble()) {
            IllegalArgumentException("Insufficient LP token to be withdrawn")
        }

        val localTokenSelector = LocalTokenSelector(serviceHub)
        val exitStates = localTokenSelector.selectTokens(amount(amount, tokenType))

        val inputsAndOutputs =  localTokenSelector.generateExit(
            exitStates = exitStates,
            amount = amount(amount, tokenType),
            changeHolder = ourIdentity
        )

        val tb = TransactionBuilder(notary)
        addTokensToRedeem(tb, inputsAndOutputs.first, inputsAndOutputs.second)

        val lpoolSession = initiateFlow(lPool)
        lpoolSession.send(mapOf(Pair("amount",amount), Pair("currency", currency)))

        val inputsAndOutputCashReceived = lpoolSession.receive<Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>>>().unwrap{ it -> it}
        addMoveTokens(tb, inputsAndOutputCashReceived.first, inputsAndOutputCashReceived.second)

        tb.verify(serviceHub)
        val initialSignedTrx = serviceHub.signInitialTransaction(tb)

        val stx = subFlow(CollectSignaturesFlow(initialSignedTrx, listOf(lpoolSession)))

        val ftx = subFlow(FinalityFlow(stx, listOf(lpoolSession)))

        subFlow(UpdateDistributionListFlow(ftx))
        return ftx
    }
}

@InitiatedBy(RedeemFeeFlow::class)
class RedeemFeeResponderFlow(
    val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call() : SignedTransaction {
        val mapsInfo = otherSession.receive<Map<*,*>>().unwrap { it } // receive token for LPManager

        val total = mapsInfo.get("amount") as Long
        val currency = mapsInfo.get("currency") as String

        val inputsAndOutputs = LocalTokenSelector(serviceHub).generateMove(
            partiesAndAmounts = listOf(Pair(otherSession.counterparty, amount(total, TokenType(currency, 2)))),
            changeHolder = ourIdentity,
            holdingKey = ourIdentity.owningKey
        )

        otherSession.send(inputsAndOutputs)



        //signing
        subFlow(object : SignTransactionFlow(otherSession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) { // Custom Logic to validate transaction.
            }
        })
        return subFlow(ReceiveFinalityFlow(otherSession))
    }

}