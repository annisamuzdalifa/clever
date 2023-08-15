package com.pool.lpm

import co.paralleluniverse.fibers.Suspendable
import com.pool.contracts.LPoolRulesContract
import com.pool.states.LPoolRules
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.TransactionBuilder
import java.lang.IllegalArgumentException

@InitiatingFlow
@StartableByRPC
class CreatePoolRulesFlow : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=Jakarta,C=ID"))
        val lPool = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager,L=Jakarta,C=ID"))!!

        if (ourIdentity != lPool) {
            throw IllegalArgumentException("Must be called from LPManager")
        }

        val poolRules = LPoolRules(
            fmid = 0.04F,
            fout = 0.51F,
            gfee = 0.3F,
            participants = listOf(ourIdentity)
        )

        val tb = TransactionBuilder(notary)
        val command = Command(LPoolRulesContract.Commands.Create(), lPool.owningKey)
        tb.addOutputState(poolRules)
        tb.addCommand(command)

        tb.verify(serviceHub)

        val initialSignedTrx = serviceHub.signInitialTransaction(tb)

        subFlow(FinalityFlow(initialSignedTrx, listOf()))

        return "Pool rules created"
    }
}