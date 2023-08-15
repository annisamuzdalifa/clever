package com.pool.states

import com.pool.contracts.CurrencyRateContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import java.time.Instant

@BelongsToContract(CurrencyRateContract::class)
data class CurrencyRateState(
    val rate : Map<Pair<String, String>, Double>,
    val lastUpdated : Instant = Instant.now(),
    private val nextActivityTime: Instant = Instant.now().plusSeconds(120),
    override val participants: List<AbstractParty>,
    val linearId: UniqueIdentifier = UniqueIdentifier()
) : SchedulableState {

    fun getRateOf(origin: String, dest: String) : Double? {
        return this.rate[Pair(origin, dest)]
    }

    fun withNewRate(newRate : Map<Pair<String, String>, Double>) : CurrencyRateState =
        CurrencyRateState(newRate, participants = this.participants, linearId = this.linearId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CurrencyRateState
        if (rate != other.rate) return false
        return true
    }

    override fun hashCode(): Int {
        var result = rate.hashCode()
        return result
    }

    override fun nextScheduledActivity(
        thisStateRef: StateRef,
        flowLogicRefFactory: FlowLogicRefFactory
    ): ScheduledActivity? {
        return ScheduledActivity(flowLogicRefFactory.create("com.pool.lpm.UpdateCurrencyRate"), nextActivityTime)
    }
}