package com.pool.contracts

import com.pool.states.CurrencyRateState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction

class CurrencyRateContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commandsOfType(CurrencyRateContract.Commands::class.java).single()
        when (command.value) {
            is Commands.Create -> {
                "There must be no input CurrencyRateState".using(tx.inputsOfType(CurrencyRateState::class.java).isEmpty())
                "There must be output CurrencyRateState".using(tx.outputsOfType(CurrencyRateState::class.java).size == 1)
            }
            is Commands.Update -> {
                "There must be input CurrencyRateState".using(tx.inputsOfType(CurrencyRateState::class.java).size == 1)
                "There must be output CurrencyRateState".using(tx.outputsOfType(CurrencyRateState::class.java).size == 1)
            }
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update : Commands
    }
}
