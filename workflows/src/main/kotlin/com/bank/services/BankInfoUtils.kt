package com.bank.services

import com.pool.FeeToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.Amount
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class BankInfoUtils (private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
    fun getTotalAssetStaked(currency: String) : Amount<TokenType> {
        return serviceHub.vaultService.tokenBalance(TokenType("LPT$currency", 2))
    }

    fun getTotalFeeEarned(currency: String) : Amount<TokenType> {
        val feeStates = serviceHub.vaultService.queryBy(FungibleToken::class.java)
        TODO()
    }
}