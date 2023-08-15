package com.bank.flows

import co.paralleluniverse.fibers.Suspendable
import com.pool.services.LiquidityPoolUtils
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class GetPoolBalance(
    val currency: String
) : FlowLogic<Amount<TokenType>>() {

    @Suspendable
    override fun call(): Amount<TokenType> {
        val lPool = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager,L=Jakarta,C=ID"))!!

        val lPoolSession = initiateFlow(lPool)
        val poolBalance = lPoolSession.sendAndReceive<Amount<TokenType>>(currency).unwrap { it }

        return poolBalance
    }
}

@InitiatedBy(GetPoolBalance::class)
class GetPoolBalanceResponder(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val currency = otherSession.receive<String>().unwrap { it }

        val servicePool = serviceHub.cordaService(LiquidityPoolUtils::class.java)
        otherSession.send(servicePool.getBalanceInPool(currency = currency))
    }
}