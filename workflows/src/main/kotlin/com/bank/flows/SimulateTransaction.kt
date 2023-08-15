package com.bank.flows

import co.paralleluniverse.fibers.Suspendable
import com.pool.services.LiquidityPoolUtils
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class SimulateTransaction(
    val amount : Long,
    val currencySource : String,
    val currencyDest: String
) : FlowLogic<LinkedHashMap<String, Any>>() {

    @Suspendable
    override fun call(): LinkedHashMap<String, Any> {
        val lPool = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager,L=Jakarta,C=ID"))!!

        val lPoolSession = initiateFlow(lPool)

        return lPoolSession.sendAndReceive<LinkedHashMap<String, Any>>(linkedMapOf("currencyDest" to currencyDest,
            "currencyOrigin" to currencySource,
            "amount" to amount)).unwrap { it }
    }
}

@InitiatedBy(SimulateTransaction::class)
class SimulateTransactionResponder(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val mapsInfo = otherSession.receive<LinkedHashMap<String, Any>>().unwrap { it }
        val respond = LinkedHashMap<String, Any>()

        val totalPutInPool = mapsInfo.get("amount") as Long
        val currencyDest = mapsInfo.get("currencyDest") as String
        val currencyOrigin = mapsInfo.get("currencyOrigin") as String

        val servicePool = serviceHub.cordaService(LiquidityPoolUtils::class.java)

        val totalExchange = servicePool.calculateTotalExchange(currencyOrigin, currencyDest, totalPutInPool)
        respond["rate"] = servicePool.exchangeRateOf(currencyOrigin, currencyDest)
        respond["totalReceived"] = totalExchange.first
        respond["totalFeeAmount"] = totalExchange.second
        respond["feePercentage"] = servicePool.calculateFeePercentage(currencyDest)

        otherSession.send(respond)
    }
}