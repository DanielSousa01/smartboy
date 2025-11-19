package com.fcul.smartboy.ui.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.transaction.Transaction
import com.fcul.smartboy.domain.user.User
import java.util.Date

@Composable
fun WalletScreen(
    transactions: List<Transaction> = sampleTransactions()
) {
    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Transactions")

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start

        ) {
            transactions.forEach {
                TransactionEntry(it)
            }
        }
    }
}

@Composable
private fun TransactionEntry(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (transaction.amount >= 0) {
                    Text("Received Caps from")
                } else {
                    Text("Sent Caps to")
                }
                Text(transaction.userDestination.name)
            }
            Column(horizontalAlignment = Alignment.End) {
                val absAmount = kotlin.math.abs(transaction.amount)
                val amountText =
                    (if (transaction.amount >= 0) "+$absAmount" else "-$absAmount") + " Caps"
                Text(amountText)
                Text(transaction.date.toString())
            }
        }
    }
}


private fun sampleTransactions(): List<Transaction> {
    val now = System.currentTimeMillis()
    return listOf(
        Transaction(
            id = 1,
            amount = -250f,
            userDestination = User(id = 1, name = "Merchant Raul"),
            date = Date(now - 3 * 60 * 60 * 1000)
        ),
        Transaction(
            id = 2,
            amount = 1200f,
            userDestination = User(id = 1, name = "Explorer's Guild"),
            date = Date(now - 1 * 24 * 60 * 60 * 1000)
        ),
        Transaction(
            id = 3,
            amount = -75f,
            userDestination = User(id = 1, name = "Ana"),
            date = Date(now - 2 * 24 * 60 * 60 * 1000)
        ),
        Transaction(
            id = 4,
            amount = 300f,
            userDestination = User(id = 1, name = "Auction House"),
            date = Date(now - 5 * 24 * 60 * 60 * 1000)
        ),
        Transaction(
            id = 5,
            amount = -999f,
            userDestination = User(id = 1, name = "Scrap Shop"),
            date = Date(now - 7 * 24 * 60 * 60 * 1000)
        )
    )
}
