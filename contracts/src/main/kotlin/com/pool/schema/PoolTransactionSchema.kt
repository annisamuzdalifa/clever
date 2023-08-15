package com.pool.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object PoolTransactionSchema

object PoolTransactionSchemaV1 : MappedSchema(
    schemaFamily = PoolTransactionSchema::class.java,
    version = 1,
    mappedTypes = listOf(PersistentPoolTransaction::class.java)
) {
    override val migrationResource: String?
        get() = "pool-transaction.changelog-master"

    @Entity
    @Table(name= "pool_transaction")
    class PersistentPoolTransaction(
        @Column(name = "sender")
        val sender : String,

        @Column(name = "receiver")
        val receiver : String,

        @Column(name = "amount_send")
        val amountSend : Double,

        @Column(name = "amount_received")
        val amountReceived : Double,

        @Column(name = "currency_origin")
        val currencyOrigin: String,

        @Column(name = "currency_dest")
        val currencyDest : String,

        @Column(name = "exchange_rate")
        val exchangeRate : Double,

        @Column(name = "fee_percentage")
        val feePercentage : Double,

        @Column(name = "fee_amount")
        val feeAmount: Double
    ) : PersistentState() {
        constructor() : this("", "", 0.0, 0.0, "", "", 0.0, 0.0, 0.0)
    }
}