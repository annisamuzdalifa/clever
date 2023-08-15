package com.pool.lpm

import co.paralleluniverse.fibers.Suspendable
import com.pool.contracts.LPoolRulesContract
import com.pool.contracts.PoolThresholdContract
import com.pool.states.LPoolRules
import com.pool.states.PoolThresholdState
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.money.FiatCurrency
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.TransactionBuilder
import java.lang.IllegalArgumentException

@InitiatingFlow
@StartableByRPC
class CreatePoolThresholdFlow : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=Jakarta,C=ID"))
        val lPool = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager,L=Jakarta,C=ID"))!!

        if (ourIdentity != lPool) {
            throw IllegalArgumentException("Must be called from LPManager")
        }

        val poolRules = PoolThresholdState(
            mapOf(
                "IDR" to amount(11200000, FiatCurrency.Companion.getInstance("IDR")),
                "SGD" to amount(100, FiatCurrency.Companion.getInstance("SGD")),
                "INR" to amount(6200, FiatCurrency.Companion.getInstance("INR"))
            ), participants = listOf(ourIdentity)
        )

        val tb = TransactionBuilder(notary)
        val command = Command(PoolThresholdContract.Commands.Create(), lPool.owningKey)
        tb.addOutputState(poolRules)
        tb.addCommand(command)

        tb.verify(serviceHub)

        val initialSignedTrx = serviceHub.signInitialTransaction(tb)

        subFlow(FinalityFlow(initialSignedTrx, listOf()))

        return "Pool Threshold created"
    }
}