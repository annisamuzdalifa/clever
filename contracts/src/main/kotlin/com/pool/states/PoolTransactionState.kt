package com.pool.states

import com.pool.contracts.PoolTransactionContract
import com.pool.schema.PoolTransactionSchemaV1
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*

@BelongsToContract(PoolTransactionContract::class)
class PoolTransactionState(
    val sender: AbstractParty,
    val receiver: AbstractParty,
    val amountSend: Amount<IssuedTokenType>,
    val amountReceived: Amount<IssuedTokenType>,
    val currencyOrigin: Currency = Currency.getInstance(amountSend.token.tokenIdentifier),
    val currencyDest: Currency = Currency.getInstance(amountReceived.token.tokenIdentifier),
    val exchangeRate: Double,
    val feePercentage: Double,
    val feeAmount: Amount<IssuedTokenType>,
    val transactionDate : Instant = Instant.now(),
    override val participants: List<AbstractParty> = listOf(sender, receiver)
) : ContractState, QueryableState {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PoolTransactionState
        if (sender != other.sender) return false
        if (receiver != other.receiver) return false
        if (amountSend != other.amountSend) return false
        if (amountReceived != other.amountReceived) return false
        if (currencyOrigin != other.currencyOrigin) return false
        if (currencyDest != other.currencyDest) return false
        if (exchangeRate != other.exchangeRate) return false
        if (feePercentage != other.feePercentage) return false
        if (feeAmount != other.feeAmount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sender.hashCode()
        result = 31 * result + receiver.hashCode()
        result = 31 * result + amountSend.hashCode()
        result = 31 * result + amountReceived.hashCode()
        result = 31 * result + currencyOrigin.hashCode()
        result = 31 * result + currencyDest.hashCode()
        result = 31 * result + exchangeRate.hashCode()
        result = 31 * result + feePercentage.hashCode()
        result = 31 * result + feeAmount.hashCode()
        return result
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        when (schema){
            is PoolTransactionSchemaV1 -> return PoolTransactionSchemaV1.PersistentPoolTransaction(
                this.sender.nameOrNull().toString(),
                this.receiver.nameOrNull().toString(),
                this.amountSend.toDecimal().toDouble(),
                this.amountReceived.toDecimal().toDouble(),
                this.currencyOrigin.currencyCode,
                this.currencyDest.currencyCode,
                this.exchangeRate,
                this.feePercentage,
                this.feeAmount.toDecimal().toDouble()
            )
            else -> throw IllegalArgumentException("Unrecognised schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PoolTransactionSchemaV1)

}