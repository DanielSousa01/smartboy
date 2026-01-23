package com.fcul.smartboy.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.transaction.Transaction
import com.fcul.smartboy.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WalletError {
    object FailedToLoadTransactions : WalletError()
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<WalletError?>(null)
    val error: StateFlow<WalletError?> = _error.asStateFlow()

    init {
        loadTransactions()
    }

    fun onDismissError() {
        _error.value = null
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val transactions = transactionRepository.getTransactions()
                _transactions.value = transactions.sortedByDescending { it.date }
            } catch (e: Exception) {
                _error.value = WalletError.FailedToLoadTransactions
            } finally {
                _isLoading.value = false
            }
        }
    }
}
