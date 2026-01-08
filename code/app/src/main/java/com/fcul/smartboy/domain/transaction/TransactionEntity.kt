package com.fcul.smartboy.domain.transaction

import com.fcul.smartboy.domain.user.User
import java.util.Date

data class TransactionEntity(
    val id: Long,
    val date: Date,
    val amount: Float,
    val userDestinationId: String,
    val userName: String
) {
    fun toTransaction(): Transaction = Transaction(
        id,
        date,
        amount,
        User(userDestinationId, userName)
    )
}