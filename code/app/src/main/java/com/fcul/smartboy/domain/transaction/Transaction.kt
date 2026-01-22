package com.fcul.smartboy.domain.transaction

import com.fcul.smartboy.domain.user.User
import java.util.Date

data class Transaction(
    val id: Long,
    val date: Date,
    val amount: Float,
    val userDestination: User
) {
    fun toEntity(): TransactionEntity = TransactionEntity(
        id = id,
        date = date,
        amount = amount,
        userDestinationId = userDestination.userId,
        userName = userDestination.username
    )
}