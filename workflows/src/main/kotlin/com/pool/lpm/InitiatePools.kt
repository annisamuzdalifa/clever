package com.pool.lpm

import co.paralleluniverse.fibers.Suspendable
import com.pool.contracts.CurrencyRateContract
import com.pool.services.OracleService
import com.pool.states.CurrencyRateState
import com.pool.states.PoolThresholdState
import com.utils.flows.BroadcastTransactionToAll
import net.corda.core.contracts.Command
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class InitiatePools(private val seconds: Long) : FlowLogic<String>() {

    constructor() : this(120)

    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=Jakarta,C=ID"))

        subFlow(CreatePoolRulesFlow())
        subFlow(CreatePoolThresholdFlow())

        val tb = TransactionBuilder(notary)

        val resultThreshold = serviceHub.vaultService.queryBy(PoolThresholdState::class.java)
        if (resultThreshold.states.isEmpty()) {
            throw IllegalStateException("There are no threshold pool found")
        }
        val listCurrency = resultThreshold.states.first().state.data.threshold.keys

        val oracleRate = serviceHub.cordaService(OracleService::class.java).updateAllCurrency(listCurrency)
        val output = CurrencyRateState(oracleRate, nextActivityTime = Instant.now().plusSeconds(seconds), participants = listOf(ourIdentity))
        val commands = Command(CurrencyRateContract.Commands.Create(), listOf(ourIdentity.owningKey))

        tb.addOutputState(output)
        tb.addCommand(commands)

        tb.verify(serviceHub)

        val initialSignedTrx = serviceHub.signInitialTransaction(tb)
        val ftx = subFlow(FinalityFlow(initialSignedTrx, listOf()))

        subFlow(BroadcastTransactionToAll(ftx))

        return "Pool initiated"
    }
}