package com.fcul.smartboy.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.cart.Cart
import com.fcul.smartboy.repository.CartRepository
import com.fcul.smartboy.repository.InventoryRepository
import com.fcul.smartboy.repository.SellingRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewmodel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val sellingRepository: SellingRepository,
    private val cartRepository: CartRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _carts = MutableStateFlow<List<Cart>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val carts: StateFlow<List<Cart>> = _carts.asStateFlow()

    val isLoading = _isLoading.asStateFlow()

    init {
        observeCarts()
    }

    private fun observeCarts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                cartRepository.observeCarts().collect { carts ->
                    _carts.value = carts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }

    }

    fun createCart() {

    }

    fun getCart(cartId: String) = carts.value.find { it.userId == cartId }


}