package com.fcul.smartboy.ui.wallet.model

import java.util.Date

data class Transaction(
    val id: Int,
    val date: Date,
    val amount: Float,
    val userDestination: User
)
