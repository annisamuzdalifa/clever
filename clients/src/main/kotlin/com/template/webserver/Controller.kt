package com.template.webserver


import com.bank.flows.*
import com.centralbank.flows.FiatCurrencyIssueFlow
import com.centralbank.flows.FiatCurrencyRedeemFlow
import com.pool.LPToken
import com.pool.flows.RedeemFeeFlow
import com.pool.flows.RedeemLPTokenFlow
import com.pool.flows.StakingFlow
import com.pool.lpm.InitiatePools
import com.pool.schema.PoolTransactionSchemaV1
import com.pool.services.LiquidityPoolUtils
import com.pool.states.CurrencyRateState
import com.pool.states.PoolTransactionState
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import net.corda.core.contracts.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.toX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.node.AppServiceHub
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.vault.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.MediaType.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder.equal
import java.math.BigDecimal
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 *  A Spring Boot Server API controller for interacting with the node via RPC.
 */

@RestController
@RequestMapping("/") // The paths for requests are relative to this base path.
class MainController(rpc: NodeRPCConnection) {

    private val proxy = rpc.proxy
    private val me = proxy.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    fun X500Name.toDisplayString(): String = BCStyle.INSTANCE.toString(this)

    /** Helpers for filtering the network map cache. */
    private fun isNotary(nodeInfo: NodeInfo) = proxy.notaryIdentities().any { nodeInfo.isLegalIdentity(it) }
    private fun isMe(nodeInfo: NodeInfo) = nodeInfo.legalIdentities.first().name == me
    private fun isNetworkMap(nodeInfo: NodeInfo) =
        nodeInfo.legalIdentities.single().name.organisation == "Network Map Service"


    /**
     * Returns the node's name.
     */
    @GetMapping(value = ["me"], produces = [APPLICATION_JSON_VALUE])
    fun whoami() = mapOf("me" to me.toString())

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GetMapping(value = ["peers"], produces = [APPLICATION_JSON_VALUE])
    fun getPeers(): Map<String, List<String>> {
        return mapOf("peers" to proxy.networkMapSnapshot()
            .filter { isNotary(it).not() && isMe(it).not() && isNetworkMap(it).not() }
            .map { it.legalIdentities.first().name.toX500Name().toDisplayString() })
    }

    @PostMapping(value = ["/initiate"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun initiatePools(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        return try {
            linkedMapOf("message" to proxy.startTrackedFlow(::InitiatePools).returnValue.getOrThrow())
        } catch (e: Throwable) {
            linkedMapOf("message" to e.message.toString())
        }
    }

    fun issueCurrencyOf(currency: String, recipient: String, amount: Long): LinkedHashMap<String, Any> {
        val requesterX500Name: CordaX500Name = CordaX500Name.parse(recipient)
        val otherParty: Party = proxy.wellKnownPartyFromX500Name(requesterX500Name)!!

        return try {
            val result: String =
                proxy.startTrackedFlow(::FiatCurrencyIssueFlow, currency, amount, otherParty).returnValue.getOrThrow()
            linkedMapOf("message" to result)

        } catch (e: Throwable) {
            linkedMapOf("error" to e.message.toString())
        }
    }

    @PostMapping(value = ["/issue-idr"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun issueRupiah(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        val amount = (request["amount"]!! as Int).toLong()
        val requester: String = request["recipient"].toString()
        val currency = "IDR"

        return issueCurrencyOf(currency, requester, amount)
    }

    @PostMapping(value = ["/issue-sgd"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun issueSGD(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        val amount = (request["amount"]!! as Int).toLong()
        val requester: String = request["recipient"].toString()
        val currency = "SGD"

        return issueCurrencyOf(currency, requester, amount)
    }

    @PostMapping(value = ["/issue-inr"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun issueINR(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        val amount = (request["amount"]!! as Int).toLong()
        val requester: String = request["recipient"].toString()
        val currency = "INR"

        return issueCurrencyOf(currency, requester, amount)
    }

    @PostMapping(value = ["/exchange"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun exchange(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        val amount = (request["amount"]!! as Int).toLong()
        val currencySource = request["currencySource"].toString()
        val currencyDest = request["currencyDest"].toString()

        return try {
            val result: String =
                proxy.startTrackedFlow(::ExchangeFlow, currencySource, currencyDest, amount).returnValue.getOrThrow()
            linkedMapOf("message" to result)

        } catch (e: Throwable) {
            linkedMapOf("error" to e.message.toString())
        }
    }

    @PostMapping(value = ["/stake"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun stake(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        val amount = (request["amount"]!! as Int).toLong()
        val currency = request["currency"].toString()

        return try {
            val result: String =
                proxy.startTrackedFlow(::StakingFlow, currency, amount).returnValue.getOrThrow()
            linkedMapOf("message" to result)

        } catch (e: Throwable) {
            linkedMapOf("error" to e.message.toString())
        }
    }

    @PostMapping(value = ["/transfer"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun transfer(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        val currencySource = request["currencySource"].toString()
        val currencyDest = request["currencyDest"].toString()
        val amount = (request["amount"]!! as Int).toLong()
        val to = request["to"].toString()
        val party = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(to))!!

        return try {
            val result: String =
                proxy.startTrackedFlow(::TransferFlow, currencySource, currencyDest, amount, party).returnValue.getOrThrow()
            linkedMapOf("message" to result)

        } catch (e: Throwable) {
            linkedMapOf("error" to e.message.toString())
        }
    }

    @PostMapping(value = ["/redeem-LPT"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun redeemLPT(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        val currency = request["currency"].toString()
        val amount = (request["amount"]!! as Int).toLong()

        return try {
            val result: SignedTransaction =
                proxy.startTrackedFlow(::RedeemLPTokenFlow, currency, amount).returnValue.getOrThrow()

            linkedMapOf("message" to "Successfully redeem $amount LPT$currency")

        } catch (e: Throwable) {
            linkedMapOf("error" to e.message.toString())
        }
    }

    @PostMapping(value = ["/redeem-Fee"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun redeemFee(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        val currency = request["currency"].toString()
        val amount = (request["amount"]!! as Int).toLong()

        return try {
            val result: SignedTransaction =
                proxy.startTrackedFlow(::RedeemFeeFlow, currency, amount).returnValue.getOrThrow()

            linkedMapOf("message" to "Successfully redeem $amount Fee$currency")

        } catch (e: Throwable) {
            linkedMapOf("error" to e.message.toString())
        }
    }

    @PostMapping(value = ["/redeem-Fiat"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun redeemFiat(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        val res = linkedMapOf<String, Any>()
        val currency = request["currency"].toString()
        val amount = (request["amount"]!! as Int).toLong()

        return try {
            val result: String =
                proxy.startTrackedFlow(::FiatCurrencyRedeemFlow, amount, currency).returnValue.getOrThrow()

            res["message"] = result
            res
        } catch (e: Throwable) {
            res["error"] = e.message.toString()
            res
        }
    }

    @PostMapping(value = ["/calculate-transaction"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun calculateTransaction(@RequestBody request: Map<String, Any>): LinkedHashMap<String, Any> {
        val res = linkedMapOf<String, Any>()
        val currencySource = request["currencySource"].toString()
        val currencyDest = request["currencyDest"].toString()
        val amount = (request["amount"]!! as Int).toLong()

        return try {
            val result: LinkedHashMap<String, Any> =
                proxy.startTrackedFlow(::SimulateTransaction, amount, currencySource, currencyDest).returnValue.getOrThrow()
            result
        } catch (e: Throwable) {
            res["error"] = e.message.toString()
            res
        }
    }

    @GetMapping(value = ["/history-rate"], produces = [APPLICATION_JSON_VALUE])
    fun getHistoryRet(): LinkedHashMap<String, Any> {
        val allcriteria =
            QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.ALL)
        val status = proxy.vaultQueryByCriteria(
            criteria = allcriteria,
            contractStateType = CurrencyRateState::class.java
        ).states.map { it.state.data }

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm");
        var res = LinkedHashMap<String, Any>()
        res["time"] = status.map { formatter.format(Date.from(it.lastUpdated)) }

        return linkedMapOf(
            "time" to status.map { formatter.format(Date.from(it.lastUpdated)) },
            "idr_sgd" to status.map{ it.rate[Pair("IDR", "SGD")] },
            "inr_sgd" to status.map{ it.rate[Pair("INR", "SGD")] },
            "idr_inr" to status.map{ it.rate[Pair("IDR", "INR")] }

        )
    }

    @GetMapping(value = ["/get-sum-fee-token"], produces = [APPLICATION_JSON_VALUE])
    fun getSumFeeToken(@RequestParam currency: String): Double {
        val criteriaFee =
            QueryCriteria.VaultCustomQueryCriteria(builder { PersistentFungibleToken::tokenIdentifier.equal("Fee$currency") })
        val sumFee = proxy.vaultQueryByCriteria(
            criteria = criteriaFee,
            contractStateType = FungibleToken::class.java
        ).states.map { it.state.data }
            .sumByDouble { it.amount.toDecimal().toDouble() }

        return sumFee
    }

    @GetMapping(value = ["/transaction-history"], produces = [APPLICATION_JSON_VALUE])
    fun getTrxHistory(): ArrayList<LinkedHashMap<String, Any>>  {
        val listData = ArrayList<LinkedHashMap<String, Any>>()
        val query = proxy.vaultQuery(
            contractStateType = PoolTransactionState::class.java
        )
        val iterator = query.states.listIterator(query.states.size)
        while (iterator.hasPrevious()) {
            val refState = iterator.previous()
            val stateData = refState.state.data
            val data = LinkedHashMap<String, Any>()

            data["date"] = stateData.transactionDate
            data["sender"] = stateData.sender.nameOrNull()!!.organisation
            data["currencyOrigin"] = stateData.currencyOrigin
            data["amountSend"] = stateData.amountSend.toDecimal().toDouble()
            data["receiver"] = stateData.receiver.nameOrNull()!!.organisation
            data["currencyDest"] = stateData.currencyDest
            data["amountReceived"] = stateData.amountReceived.toDecimal().toDouble()
            data["rate"] = stateData.exchangeRate
            data["feePercentage"] = stateData.feePercentage
            data["feeAmount"] = stateData.feeAmount.toDecimal().toDouble()

            listData.add(data)
        }

        return listData
    }

    @GetMapping(value = ["/inflow-outflow"], produces = [APPLICATION_JSON_VALUE])
    fun getInflowOutflow(@RequestParam currency: String): LinkedHashMap<String, Any> {
        val res = LinkedHashMap<String, Any>()
        val criteriaInflowByCurrency =
            QueryCriteria.VaultCustomQueryCriteria(builder { PoolTransactionSchemaV1.PersistentPoolTransaction::currencyDest.equal(currency) })
        val criteriaInflow = QueryCriteria.VaultCustomQueryCriteria(builder { PoolTransactionSchemaV1.PersistentPoolTransaction::receiver.equal(me.toString()) })

        val criteriaOutflowByCurrency =
            QueryCriteria.VaultCustomQueryCriteria(builder { PoolTransactionSchemaV1.PersistentPoolTransaction::currencyOrigin.equal(currency) })
        val criteriaOutflow = QueryCriteria.VaultCustomQueryCriteria(builder { PoolTransactionSchemaV1.PersistentPoolTransaction::sender.equal(me.toString()) })

        // Get ourIdentity as receiver AND in specific currency
        val queryInflow = proxy.vaultQueryByCriteria(
            contractStateType = PoolTransactionState::class.java,
            criteria = criteriaInflow.and(criteriaInflowByCurrency)
        )

        val queryOutflow = proxy.vaultQueryByCriteria(
            contractStateType = PoolTransactionState::class.java,
            criteria = criteriaOutflowByCurrency.and(criteriaOutflow)
        )

        val formatter = SimpleDateFormat("yyyy-MM-dd HH");

        val inflowData = queryInflow.states.map { it.state.data }.groupBy { formatter.format(Date.from(it.transactionDate)) }.mapValues { it -> it.value.sumByDouble { it.amountReceived.quantity*it.amountReceived.displayTokenSize.toDouble() } }
        val outflowData = queryOutflow.states.map { it.state.data }.groupBy { formatter.format(Date.from(it.transactionDate)) }.mapValues { it -> it.value.sumByDouble { it.amountSend.quantity*it.amountSend.displayTokenSize.toDouble() } }

        res["inflow"] = linkedMapOf("timestamp" to inflowData.keys, "data" to inflowData.entries.map { it.value })
        res["outflow"] = linkedMapOf("timestamp" to outflowData.keys, "data" to outflowData.entries.map { it.value })


        return res
    }

    @GetMapping(value = ["/get-pool-info"], produces = [APPLICATION_JSON_VALUE])
    fun getPoolInfo(): ResponseEntity<LinkedHashMap<String, Any>> {
        val res = LinkedHashMap<String, Any>()
        val pool = LinkedHashMap<String, Double>()
        val fee = LinkedHashMap<String, Double>()

        val queryCurrency = proxy.vaultQuery(CurrencyRateState::class.java)
        val currency = queryCurrency.states.first().state.data.rate.keys.map { it.first }.toSet()

        for (index in currency) {
            val poolBalance = proxy.startFlow(::GetPoolBalance, index).returnValue.get().toDecimal().toDouble()
            pool.put(index, poolBalance)

            val currentFee =
                proxy.startFlow(::GetCurrentFee, index).returnValue.get()
            fee[index] = currentFee
        }

        res.put("pool", pool)
        res.put("fee", fee)
        return ResponseEntity.ok().body(res)
    }

    //Sum LP Token that participant have in pool
    @GetMapping(value = ["/get-sum-lp-token"], produces = [APPLICATION_JSON_VALUE])
    fun getSumLpToken(@RequestParam currency: String): Double {
        val currencyCriteria =
            QueryCriteria.VaultCustomQueryCriteria(builder { PersistentFungibleToken::tokenIdentifier.equal("LPT$currency") })
        val sumLpToken = proxy.vaultQueryByCriteria(
            criteria = currencyCriteria.and(currencyCriteria),
            contractStateType = FungibleToken::class.java
        ).states.map { it.state.data }
            .sumByDouble { it.amount.toDecimal().toDouble() }

        return sumLpToken
    }

    //Exchange Dashboard
    @GetMapping(value = ["/get-exchange-info"], produces = [APPLICATION_JSON_VALUE])
    fun getExchangeInfo(): ResponseEntity<LinkedHashMap<String, Any>> {
        val res = LinkedHashMap<String, Any>()
        val rate = mutableListOf<LinkedHashMap<String, Any>>()

        val queryCurrency = proxy.vaultQuery(CurrencyRateState::class.java)
        val currency = queryCurrency.states.first().state.data.rate.keys.map { it.first }.toSet()
        val pairCurrency = queryCurrency.states.first().state.data.rate.map {"${it.key.first}/${it.key.second}" to it.value }

        pairCurrency.forEach {
            val rate2 = LinkedHashMap<String, Any>()
            rate2["pair"] = it.first
            rate2["price"] = it.second
            rate.add(rate2)
        }

        val trxFee = currency.associate{ Pair(it, proxy.startFlow(::GetCurrentFee, it).returnValue.get())}

        res.put("rate", rate)
        res.put("fee", trxFee)

        return ResponseEntity.ok().body(res)
    }

    //Balance of participant that haven't staked
    @GetMapping(value = ["/get-bank-balance"], produces = [APPLICATION_JSON_VALUE])
    fun getBankBalance(@RequestParam currency: String): Double {
        val currencyCriteria =
            QueryCriteria.VaultCustomQueryCriteria(builder { PersistentFungibleToken::tokenIdentifier.equal(currency) })
        val bankBalance = proxy.vaultQueryByCriteria(
            criteria = currencyCriteria,
            contractStateType = FungibleToken::class.java
        ).states.map { it.state.data }
            .sumByDouble { it.amount.toDecimal().toDouble() }

        return bankBalance
    }

    //Bank Information For Staking Main Dashboard
    @GetMapping(value = ["/bank-info"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getBankInfo(): ResponseEntity<LinkedHashMap<String, Any>> {
        val res = LinkedHashMap<String, Any>()
        val bankBalance = LinkedHashMap<String, Double>()
        val stakedBankBalance = LinkedHashMap<String, Double>()
        val feeBankInPool = LinkedHashMap<String, Double>()
        val rate = mutableListOf<LinkedHashMap<String, Any>>()

        val queryCurrency = proxy.vaultQuery(CurrencyRateState::class.java)
        val currency = queryCurrency.states.first().state.data.rate.keys.map { it.first }.toSet()
        val pairCurrency = queryCurrency.states.first().state.data.rate.map {"${it.key.first}/${it.key.second}" to it.value }

        for (index in currency) {
            val currencyCriteria =
                QueryCriteria.VaultCustomQueryCriteria(builder { PersistentFungibleToken::tokenIdentifier.equal(index) })
            val balance = proxy.vaultQueryByCriteria(
                criteria = currencyCriteria,
                contractStateType = FungibleToken::class.java
            ).states.map {
                it.state.data
            }.sumByDouble {
                it.amount.toDecimal().toDouble()
            }
            bankBalance.put(index, balance)

            val currencyLpCriteria =
                QueryCriteria.VaultCustomQueryCriteria(builder { PersistentFungibleToken::tokenIdentifier.equal("LPT$index") })
            val stakedBalance = proxy.vaultQueryByCriteria(
                criteria = currencyLpCriteria,
                contractStateType = FungibleToken::class.java
            ).states.map {
                it.state.data
            }.sumByDouble {
                it.amount.toDecimal().toDouble()
            }
            stakedBankBalance.put(index, stakedBalance)

            val feeCurrencyCriteria =
                QueryCriteria.VaultCustomQueryCriteria(builder { PersistentFungibleToken::tokenIdentifier.equal("Fee$index") })
            val feeInPool = proxy.vaultQueryByCriteria(
                criteria = feeCurrencyCriteria,
                contractStateType = FungibleToken::class.java
            ).states.map {
                it.state.data
            }.sumByDouble {
                it.amount.toDecimal().toDouble()
            }
            feeBankInPool.put(index, feeInPool)
        }

        //val rateCurrency = LinkedHashMap<String, Any>()

        pairCurrency.forEach {
            val rate2 = LinkedHashMap<String, Any>()
            rate2["pair"] = it.first
            rate2["price"] = it.second
            rate.add(rate2)
        }


        res.put("balance", bankBalance)
        res.put("staked", stakedBankBalance)
        res.put("fee", feeBankInPool)
        res.put("rate",rate)
        return ResponseEntity.ok().body(res)
    }
}
