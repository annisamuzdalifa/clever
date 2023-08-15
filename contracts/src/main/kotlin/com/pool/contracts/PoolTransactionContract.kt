package com.pool.contracts

import com.pool.states.PoolTransactionState
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class PoolTransactionContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.pool.contracts.CrossBorderPaymentContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commandsOfType(Commands::class.java).single()
        val outputCrossBorderPayment = tx.outputsOfType<PoolTransactionState>()
        "There must be no input of PoolTransactionState state".using(tx.inputsOfType<PoolTransactionState>().isEmpty())
        "There must be output of PoolTransactionState state".using(outputCrossBorderPayment.isNotEmpty())

        val outputState = outputCrossBorderPayment.single()



        when (command.value) {
            is Commands.Transfer -> requireThat {
                val inputsCash = tx.inputsOfType<FungibleToken>()
                "Sender and Receiver must not be the same entity".using(outputState.sender != outputState.receiver)

            }
            is Commands.Exchange -> requireThat {
                val inputsCash = tx.inputsOfType<FungibleToken>()

                "Sender and Receiver must be the same entity".using(outputState.sender == outputState.receiver)
            }
        }

    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Transfer : Commands
        class Exchange : Commands
    }

}
