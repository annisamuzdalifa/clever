package com.pool.contracts

import co.paralleluniverse.fibers.Suspendable
import com.pool.states.LPoolRules
import com.pool.states.PoolThresholdState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction

class PoolThresholdContract : Contract {
    @Suspendable
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commandsOfType(PoolThresholdContract.Commands::class.java).single()
        when (command.value) {
            is PoolThresholdContract.Commands.Create -> {
                "There must be no input PoolThresholdState".using(tx.inputsOfType(PoolThresholdState::class.java).isEmpty())
                "There must be output PoolThresholdState".using(tx.outputsOfType(PoolThresholdState::class.java).size == 1)
            }

            is PoolThresholdContract.Commands.Update -> {
                "There must be input PoolThresholdState".using(tx.inputsOfType(PoolThresholdState::class.java).size == 1)
                "There must be output PoolThresholdState".using(tx.outputsOfType(PoolThresholdState::class.java).size == 1)
            }
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update : Commands
    }

}
