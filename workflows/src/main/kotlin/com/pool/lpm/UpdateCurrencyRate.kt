package com.pool.lpm

import co.paralleluniverse.fibers.Suspendable
import com.pool.contracts.CurrencyRateContract
import com.pool.services.OracleService
import com.pool.states.CurrencyRateState
import com.pool.states.PoolThresholdState
import com.utils.flows.BroadcastTransactionToAll
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.TransactionBuilder
import java.lang.IllegalArgumentException

@InitiatingFlow
@SchedulableFlow
class UpdateCurrencyRate : FlowLogic<String>() {
    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=Jakarta,C=ID"))
        val lPool = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager,L=Jakarta,C=ID"))!!

        if(ourIdentity != lPool) {
            throw IllegalArgumentException("Must be called from LPManager")
        }

        val tb = TransactionBuilder(notary)
        val resultCurrencyRate = serviceHub.vaultService.queryBy(CurrencyRateState::class.java)

        val input = resultCurrencyRate.states.single()
        val commands = Command(CurrencyRateContract.Commands.Update(), ourIdentity.owningKey)

        val resultThreshold = serviceHub.vaultService.queryBy(PoolThresholdState::class.java)
        if (resultThreshold.states.isEmpty()) {
            throw IllegalStateException("There are no threshold pool found")
        }
        val listCurrency = resultThreshold.states.first().state.data.threshold.keys

        val oracleRate = serviceHub.cordaService(OracleService::class.java).updateAllCurrency(listCurrency)
        val output = input.state.data.withNewRate(oracleRate)

        tb.addInputState(input)
        tb.addOutputState(output)
        tb.addCommand(commands)

        tb.verify(serviceHub)

        val initialSignedTrx = serviceHub.signInitialTransaction(tb)
        val ftx = subFlow(FinalityFlow(initialSignedTrx, listOf()))

        subFlow(BroadcastTransactionToAll(ftx))

        return "Successfully updating CurrencyRateState"
    }
}