package com.pool.flows

import com.pool.FeeToken
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import java.util.*

fun feeIssuance(
    serviceHub: ServiceHub,
    ourIdentity : Party,
    totalFeeAmount : Double,
    currency: String
) : List<FungibleToken> {

    val lPTokenCriteria = QueryCriteria.VaultCustomQueryCriteria(builder {
        PersistentFungibleToken::tokenIdentifier.equal("LPT$currency")
    })
    // List LPToken
    val listLPToken = serviceHub.vaultService.queryBy<FungibleToken>(lPTokenCriteria).states.map { it.state.data }
    val totalTokenStaked = listLPToken.sumByDouble { it.amount.quantity*it.amount.displayTokenSize.toDouble() }

    val percentagePerStakeholder  = listLPToken.groupBy { it.holder }
        .mapValues { it -> it.value.sumByDouble { it.amount.quantity*it.amount.displayTokenSize.toDouble() } }
        .mapValues { it.value / totalTokenStaked }

    return percentagePerStakeholder.map {
        FungibleTokenBuilder()
            .withAmount(totalFeeAmount*it.value)
            .ofTokenType(FeeToken(Currency.getInstance(currency)))
            .issuedBy(ourIdentity)
            .heldBy(it.key.toParty(serviceHub))
            .buildFungibleToken()
    }
}