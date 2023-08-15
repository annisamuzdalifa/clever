package com.pool

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.util.*

data class FeeToken(
    val currency: Currency,
    override val fractionDigits: Int = 6,
    override val tokenIdentifier: String = "Fee${currency.currencyCode}"
) : TokenType(tokenIdentifier, fractionDigits) {
}