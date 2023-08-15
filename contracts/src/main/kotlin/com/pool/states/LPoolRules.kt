package com.pool.states

import com.pool.contracts.LPoolRulesContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty

@BelongsToContract(LPoolRulesContract::class)
data class LPoolRules(
    val fmid : Float,
    val fout : Float,
    val gfee : Float,
    override val participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : ContractState, LinearState {
}