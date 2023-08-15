package com.pool.states

import antlr.Token
import com.pool.contracts.CurrencyRateContract
import com.pool.contracts.PoolThresholdContract
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import java.time.Instant

@BelongsToContract(PoolThresholdContract::class)
data class PoolThresholdState(
    val threshold : Map<String, Amount<TokenType>>,
    val lastUpdated : Instant = Instant.now(),
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : ContractState, LinearState {

    fun getThresholdOf(currency: String) : Amount<TokenType>? {
        return this.threshold[currency]
    }

    fun withNewThreshold(newthreshold : Map<String, Amount<TokenType>>) : PoolThresholdState =
        PoolThresholdState(newthreshold, participants = this.participants, linearId = this.linearId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PoolThresholdState
        if (threshold != other.threshold) return false
        return true
    }

    override fun hashCode(): Int {
        var result = threshold.hashCode()
        return result
    }
}