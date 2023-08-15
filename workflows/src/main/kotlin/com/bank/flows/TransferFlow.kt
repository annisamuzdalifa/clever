package com.bank.flows

import co.paralleluniverse.fibers.Suspendable
import com.pool.contracts.PoolTransactionContract
import com.pool.flows.feeIssuance
import com.pool.services.LiquidityPoolUtils
import com.pool.states.PoolTransactionState
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.selection.memory.selector.LocalTokenSelector
import com.r3.corda.lib.tokens.workflows.flows.issue.addIssueTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.lang.IllegalArgumentException

@InitiatingFlow
@StartableByRPC
class TransferFlow(
    val currencySource: String,
    val currencyDest: String,
    val amount: Long,
    val to: Party
) : FlowLogic<String>() {
    companion object {
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on Transfer State.")
        object ADDING_CASH_SOURCE : ProgressTracker.Step("Adding fiat currency to transaction builder")
        object ADDING_CASH_RECEIVED : ProgressTracker.Step("Add cash from pool to transaction builder")
        object ISSUING_FEE : ProgressTracker.Step("Issue fee for each stakeholder")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        object BROADCAST_TO_OBSERVER : ProgressTracker.Step("Sending signed transaction to Observer Node.")

        fun tracker() = ProgressTracker(
            ADDING_CASH_SOURCE,
            ADDING_CASH_RECEIVED,
            ISSUING_FEE,
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION,
            BROADCAST_TO_OBSERVER
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=Jakarta,C=ID"))
        val lPool = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager,L=Jakarta,C=ID"))!!
        val tokenTypeSrc = TokenType(currencySource, 2)

        // Check source balance
        val balance = serviceHub.vaultService.tokenBalance(tokenTypeSrc)
        if (balance.quantity.times(balance.displayTokenSize.toDouble()) < amount.toDouble()) {
            IllegalArgumentException("Insufficient $currencySource balance")
        }

        progressTracker.currentStep = GENERATING_TRANSACTION
        val tb = TransactionBuilder(notary)

        progressTracker.currentStep = ADDING_CASH_SOURCE
        val localTokenSelector = LocalTokenSelector(serviceHub)
        val inputAndOutputs = localTokenSelector.generateMove(
            partiesAndAmounts = listOf(Pair(lPool, amount(amount, tokenTypeSrc))),
            changeHolder = ourIdentity,
            holdingKey = ourIdentity.owningKey
        )
        addMoveTokens(tb, inputAndOutputs.first, inputAndOutputs.second)

        val lpoolSession = initiateFlow(lPool)

        progressTracker.currentStep = ADDING_CASH_RECEIVED
        val amountReceived = lpoolSession.sendAndReceive<Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>>>(
            mapOf(Pair("amount", amount), Pair("currencyDest", currencyDest), Pair("currencyOrigin", currencySource),  Pair("recipient", to)))
            .unwrap { it }
        addMoveTokens(tb, amountReceived.first, amountReceived.second)

        progressTracker.currentStep = ISSUING_FEE
        val listFee = lpoolSession.receive<List<FungibleToken>>().unwrap{it}
        addIssueTokens(tb, listFee)

        val mapInfo = lpoolSession.receive<Map<String, Double>>().unwrap { it }

        val outputPool = PoolTransactionState(
            ourIdentity,
            to,
            inputAndOutputs.second.first().amount,
            amountReceived.second.first().amount,
            exchangeRate = mapInfo["rate"]!!,
            feePercentage = mapInfo["fees"]!!,
            feeAmount = amount(mapInfo["amountFee"]!!, listFee.first().issuedTokenType),
            participants = listOf(ourIdentity, to, lPool)
        )
        val command = Command(PoolTransactionContract.Commands.Transfer(), outputPool.participants.map { it.owningKey }+lPool.owningKey)
        tb.addOutputState(outputPool)
        tb.addCommand(command)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        tb.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val initialSignedTrx = serviceHub.signInitialTransaction(tb)

        // Only lpm because of exchange transaction
        progressTracker.currentStep = GATHERING_SIGS
        val toSession = initiateFlow(to)
        val stx = subFlow(CollectSignaturesFlow(initialSignedTrx, listOf(lpoolSession, toSession)))

        progressTracker.currentStep = FINALISING_TRANSACTION
        val ftx = subFlow(FinalityFlow(stx, listOf(lpoolSession,toSession)))

        subFlow(UpdateDistributionListFlow(ftx))
        return "Successfully exchange $currencySource $amount with $currencyDest ${amountReceived.second.first().amount.quantity/100}"
    }
}

@InitiatedBy(TransferFlow::class)
class TransferResponderFlow(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val lPool = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager,L=Jakarta,C=ID"))!!

        if (ourIdentity == lPool) {

            val mapsInfo = otherSession.receive<Map<*,*>>().unwrap { it } // receive token for LPManager

            val totalPutInPool = mapsInfo.get("amount") as Long
            val currencyDest = mapsInfo.get("currencyDest") as String
            val party = mapsInfo.get("recipient") as Party
            val currencyOrigin = mapsInfo.get("currencyOrigin") as String

            val servicePool = serviceHub.cordaService(LiquidityPoolUtils::class.java)

            val totalExchange = servicePool.calculateTotalExchange(currencyOrigin, currencyDest, totalPutInPool)
            val totalReceived = totalExchange.first
            val totalFeeAmount = totalExchange.second

            // Adding Cash From Pool
            val inputsAndOutputs = LocalTokenSelector(serviceHub).generateMove(
                partiesAndAmounts = listOf(Pair(party, amount(totalReceived, TokenType(currencyDest, 2)))),
                changeHolder = ourIdentity,
                holdingKey = ourIdentity.owningKey
            )
            otherSession.send(inputsAndOutputs)

            // Issued Token
            val inputs : List<FungibleToken> = feeIssuance(serviceHub, ourIdentity, totalFeeAmount, currencyDest)
            otherSession.send(inputs)

            otherSession.send(mapOf(
                Pair("rate", servicePool.exchangeRateOf(currencyOrigin, currencyDest)),
                Pair("fees", servicePool.calculateFeePercentage(currencyDest)),
                Pair("amountFee", totalFeeAmount)
            ))
        }

        //signing
        subFlow(object : SignTransactionFlow(otherSession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) { // Custom Logic to validate transaction.
            }
        })
        return if(ourIdentity == lPool){
            subFlow(
                ReceiveFinalityFlow(otherSideSession = otherSession,
                    statesToRecord = StatesToRecord.ALL_VISIBLE)
            )
        } else {
            subFlow(
                ReceiveFinalityFlow(otherSideSession = otherSession)
            )
        }
    }

}