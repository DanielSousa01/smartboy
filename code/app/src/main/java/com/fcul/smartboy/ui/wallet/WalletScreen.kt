package com.fcul.smartboy.ui.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.transaction.Transaction
import com.fcul.smartboy.ui.common.ErrorSnackbar
import com.fcul.smartboy.ui.wallet.components.TransactionEntry

@Composable
fun WalletScreen(
    transactions: List<Transaction>,
    isLoading: Boolean,
    error: WalletError?,
    onDismissError: () -> Unit,
) {
    val errorMessage = when (error) {
        is WalletError.FailedToLoadTransactions -> stringResource(R.string.error_wallet_failed_to_load_transactions)
        else -> null
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                transactions.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your transaction history will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.transactions),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(transactions, key = { it.id }) { transaction ->
                                TransactionEntry(transaction)
                            }
                        }
                    }
                }
            }
        }

        ErrorSnackbar(
            errorMessage = errorMessage,
            onDismissError = onDismissError
        )
    }
}
