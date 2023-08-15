package com.pool.contracts

import com.pool.states.CurrencyRateState
import com.pool.states.LPoolRules
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction

class LPoolRulesContract : Contract{
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.pool.contracts.LPoolRulesContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commandsOfType(Commands::class.java).single()
        when (command.value) {
            is Commands.Create -> {
                "There must be no input LPoolRules".using(tx.inputsOfType(LPoolRules::class.java).isEmpty())
                "There must be output LPoolRules".using(tx.outputsOfType(LPoolRules::class.java).size == 1)
            }
            is Commands.Update -> {
                "There must be input LPoolRules".using(tx.inputsOfType(LPoolRules::class.java).size == 1)
                "There must be output LPoolRules".using(tx.outputsOfType(LPoolRules::class.java).size == 1)
            }
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Update : Commands
    }
}
