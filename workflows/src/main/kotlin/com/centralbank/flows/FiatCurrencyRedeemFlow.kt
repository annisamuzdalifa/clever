package com.centralbank.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.selection.memory.selector.LocalTokenSelector
import com.r3.corda.lib.tokens.workflows.flows.redeem.addTokensToRedeem
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class FiatCurrencyRedeemFlow(
    val amount: Long,
    val currency: String
) : FlowLogic<String>() {
    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=Jakarta,C=ID"))

        val selector = LocalTokenSelector(serviceHub)
        val exitStates = selector.selectTokens(amount(amount, TokenType(currency, 2)))

        val (inputs, changeOutput) =  selector.generateExit(
            exitStates = exitStates,
            amount = amount(amount, TokenType(currency, 2)),
            changeHolder = ourIdentity
        )

        val tb = TransactionBuilder(notary)
        addTokensToRedeem(tb, inputs, changeOutput)

        tb.verify(serviceHub)
        val initialSignedTrx = serviceHub.signInitialTransaction(tb)

        val sessions = inputs.map{ initiateFlow(it.state.data.issuer)}
        val stx = subFlow(CollectSignaturesFlow(initialSignedTrx, sessions))

        val ftx = subFlow(FinalityFlow(stx, sessions))

        subFlow(UpdateDistributionListFlow(ftx))


        return "Redeem $amount $currency"
    }
}


@InitiatedBy(FiatCurrencyRedeemFlow::class)
class FiatCurrencyRedeemResponderFlow(
    val otherSession: FlowSession
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
            }
        }
        val transaction = subFlow(signTransactionFlow)
        return subFlow(
            ReceiveFinalityFlow(
                otherSession,
                expectedTxId = transaction.id,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )
    }
}