package com.fcul.smartboy.domain.transaction

import com.fcul.smartboy.domain.user.User
import java.util.Date

data class TransactionEntity(
    val id: Long = 0,
    val date: Date = Date(),
    val amount: Float = 0f,
    val userDestinationId: String = "",
    val userName: String = ""
) {
    fun toTransaction(): Transaction = Transaction(
        id,
        date,
        amount,
        User(userDestinationId, userName)
    )
}