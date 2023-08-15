package com.bank.flows

import co.paralleluniverse.fibers.Suspendable
import com.pool.services.LiquidityPoolUtils
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class GetCurrentFee(
    val currencyDest: String
) : FlowLogic<Double>() {

    @Suspendable
    override fun call(): Double {
        val lPool = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager,L=Jakarta,C=ID"))!!

        val lPoolSession = initiateFlow(lPool)

        return lPoolSession.sendAndReceive<Double>(currencyDest).unwrap { it }
    }
}

@InitiatedBy(GetCurrentFee::class)
class GetCurrentFeeResponder(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val currency = otherSession.receive<String>().unwrap { it }

        val servicePool = serviceHub.cordaService(LiquidityPoolUtils::class.java)
        otherSession.send(servicePool.calculateFeePercentage(currency))
    }
}