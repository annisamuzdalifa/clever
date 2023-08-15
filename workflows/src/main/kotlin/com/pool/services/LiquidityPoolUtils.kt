package com.pool.services


import com.pool.states.CurrencyRateState
import com.pool.states.LPoolRules
import com.pool.states.PoolThresholdState
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.utilities.heldTokensByToken
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.sumByLong
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy

import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import java.util.*

@CordaService
class LiquidityPoolUtils (private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
    companion object {
        val log = loggerFor<LiquidityPoolUtils>()
    }

    fun getBalanceInPool(currency: String, amount: Amount<TokenType> = amount(0, TokenType(currency, 2))) : Amount<TokenType> {
        val lpm = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse("O=LPManager, L=Jakarta, C=ID"))
        val holderCriteria = QueryCriteria.VaultCustomQueryCriteria(builder {
            PersistentFungibleToken::holder.equal(lpm)
        })
        val currencyCriteria = QueryCriteria.VaultCustomQueryCriteria(builder {
            PersistentFungibleToken::tokenIdentifier.equal(currency)
        })

        val feeCriteria = QueryCriteria.VaultCustomQueryCriteria(builder {
            PersistentFungibleToken::tokenIdentifier.equal("Fee$currency")
        })

        val listFeeToken = serviceHub.vaultService.queryBy<FungibleToken>(feeCriteria).states.map { it.state.data }
        val listToken = serviceHub.vaultService.queryBy<FungibleToken>(holderCriteria.and(currencyCriteria)).states.map { it.state.data }
        log.info("List Fee Token")
        log.info(listFeeToken.toString())
        log.info("List Token")
        log.info(listToken.toString())

        return if (listToken.isNotEmpty()) {
            amount(
                amount = listToken.sumByDouble { it.amount.quantity*0.01 }
                        + amount.quantity*0.01
                        - listFeeToken.sumByDouble { it.amount.quantity*0.000001 },
                token = listToken.first().tokenType)
        } else {
            amount(0, TokenType(currency, 2))
        }
    }

    fun calculateY(pmarket: Double, c : Double, a : Double, balanceX : Double) : Double {
        return c - (pmarket* (balanceX - a))
    }

    fun calculateNewA(newPMarket: Double, balanceX: Double, balanceY: Double, c: Double) : Double {
        return balanceX - ((c-balanceY)/newPMarket)
    }

    fun calculateTotalExchange(origin: String, dest: String, amount: Long) : Pair<Double, Double> {
        val totalExchange = amount*this.exchangeRateOf(origin, dest)
        val totalFeeAmount = calculateFeePercentage(dest)/100*totalExchange
        val totalReceived = totalExchange - totalFeeAmount

        return Pair(totalReceived, totalFeeAmount)
    }

    fun exchangeRateOf(origin: String, dest: String): Double {
        val resultRate = serviceHub.vaultService.queryBy(CurrencyRateState::class.java)
        if (resultRate.states.isEmpty()) {
            throw IllegalStateException("There are no currency rate pool found")
        }
        val rateState = resultRate.states.first().state.data

        return rateState.getRateOf(origin, dest)!!
    }

    fun calculateFeePercentage(currency: String) : Double {
        val resultRules = serviceHub.vaultService.queryBy(LPoolRules::class.java)
        if (resultRules.states.isEmpty()) {
            throw IllegalStateException("There are no liquidity pool rules found")
        }
        val poolRules = resultRules.states.first().state.data
        val poolBalance = this.getBalanceInPool(currency)
        val resultThreshold = serviceHub.vaultService.queryBy(PoolThresholdState::class.java)
        if (resultThreshold.states.isEmpty()) {
            throw IllegalStateException("There are no threshold pool found")
        }
        val poolThreshold = resultThreshold.states.first().state.data
        val thresholdRupiah = poolThreshold.getThresholdOf(currency)!!

        return this.calculateFee(
            this.calculateG(
                poolRules.gfee,
                thresholdRupiah.quantity.times(thresholdRupiah.displayTokenSize.toDouble()),
                poolBalance.quantity.times(poolBalance.displayTokenSize.toDouble())),
            poolRules.fmid,
            poolRules.fout
        )
    }


    fun calculateG(
        gfee : Float,
        threshold : Double,
        poolBalance : Double
    ) : Float {
        return if(poolBalance >= threshold) {
            1.0F
        } else {
            (gfee/(gfee + 1 - (poolBalance/threshold))).toFloat()
        }
    }

    fun calculateFee(
        g: Float,
        fmid : Float,
        fout : Float
    ) : Double {
        return (g*fmid) + ((1-g)*fout).toDouble()
    }

}
