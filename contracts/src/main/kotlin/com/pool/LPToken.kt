package com.pool

import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
data class LPToken(
    val currency: Currency,
    override val tokenIdentifier: String = "LPT${currency.currencyCode}",
    override val fractionDigits : Int = 2
) : TokenType(tokenIdentifier, fractionDigits) {

    fun toTokenType() : TokenType {
        return TokenType(this.tokenIdentifier, this.fractionDigits)
    }
}