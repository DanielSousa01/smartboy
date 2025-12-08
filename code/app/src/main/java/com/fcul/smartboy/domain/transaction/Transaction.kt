package com.fcul.smartboy.domain.transaction

import com.fcul.smartboy.domain.user.User
import java.util.Date

data class Transaction(
    val id: Long,
    val date: Date,
    val amount: Float,
    val userDestination: User
)