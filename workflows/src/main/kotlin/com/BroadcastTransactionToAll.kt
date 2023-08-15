package com.utils.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
class BroadcastTransactionToAll(val stx: SignedTransaction) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val everyone = serviceHub.networkMapCache.allNodes.flatMap { it.legalIdentities }
        val everyoneButMeAndNotary = everyone.filter { serviceHub.networkMapCache.isNotary(it).not() } - ourIdentity
        val sessions = everyoneButMeAndNotary.map { initiateFlow(it) }

        // Send to all participants
        sessions.forEach { subFlow(SendTransactionFlow(it, stx)) }
    }

}

@InitiatedBy(BroadcastTransactionToAll::class)
class RecordTransaction(val otherSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val flow = ReceiveTransactionFlow(
                otherSideSession = otherSession,
                checkSufficientSignatures = true,
                statesToRecord = StatesToRecord.ALL_VISIBLE
        )
        subFlow(flow)
    }
}