package com.fcul.smartboy.ui.cart

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.cart.Cart
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.domain.transaction.Transaction
import com.fcul.smartboy.domain.user.User
import com.fcul.smartboy.repository.CartRepository
import com.fcul.smartboy.repository.InventoryRepository
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.repository.SellingRepository
import com.fcul.smartboy.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

@HiltViewModel
class CartViewmodel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val sellingRepository: SellingRepository,
    private val cartRepository: CartRepository,
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _currentCart = MutableStateFlow<Cart?>(null)
    val currentCart: StateFlow<Cart?> = _currentCart.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
    val qrCodeBitmap: StateFlow<Bitmap?> = _qrCodeBitmap.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadOrCreateCart()
    }

    private fun loadOrCreateCart() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid
                val userName = auth.currentUser?.displayName ?: "Guest"

                if (userId != null) {
                    // Try to load existing cart
                    val existingCart = cartRepository.read(userId.hashCode().toLong())

                    if (existingCart != null) {
                        _currentCart.value = existingCart
                        Log.d("CartViewModel", "Loaded existing cart with ${existingCart.items.size} items")
                    } else {
                        // Create new empty cart
                        val newCart = Cart(
                            userId = userId,
                            userName = userName,
                            totalPrice = 0,
                            items = emptyList()
                        )
                        _currentCart.value = newCart
                        Log.d("CartViewModel", "Created new empty cart")
                    }
                }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load cart: ${e.message}"
                Log.e("CartViewModel", "Failed to load cart", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addItemToCart(item: SellingItem) {
        viewModelScope.launch {
            try {
                val currentCart = _currentCart.value ?: return@launch
                val userId = auth.currentUser?.uid ?: return@launch

                // Check if item already exists in cart
                val existingItemIndex = currentCart.items.indexOfFirst { it.id == item.id }

                val updatedItems = if (existingItemIndex >= 0) {
                    // Update quantity of existing item
                    currentCart.items.toMutableList().apply {
                        val existingItem = this[existingItemIndex]
                        this[existingItemIndex] = existingItem.copyItem(
                            quantity = existingItem.quantity + 1
                        )
                    }
                } else {
                    // Add new item to cart
                    currentCart.items + item.copyItem(quantity = 1)
                }

                val updatedCart = currentCart.copy(
                    items = updatedItems,
                    totalPrice = calculateTotalPrice(updatedItems)
                )

                // Save to repository
                cartRepository.update(userId.hashCode().toLong(), updatedCart)
                _currentCart.value = updatedCart

                Log.d("CartViewModel", "Added item to cart: ${item.name}")
            } catch (e: Exception) {
                _error.value = "Failed to add item: ${e.message}"
                Log.e("CartViewModel", "Failed to add item to cart", e)
            }
        }
    }

    fun removeItemFromCart(itemId: Long) {
        viewModelScope.launch {
            try {
                val currentCart = _currentCart.value ?: return@launch
                val userId = auth.currentUser?.uid ?: return@launch

                val updatedItems = currentCart.items.filter { it.id != itemId }
                val updatedCart = currentCart.copy(
                    items = updatedItems,
                    totalPrice = calculateTotalPrice(updatedItems)
                )

                cartRepository.update(userId.hashCode().toLong(), updatedCart)
                _currentCart.value = updatedCart

                Log.d("CartViewModel", "Removed item from cart: $itemId")
            } catch (e: Exception) {
                _error.value = "Failed to remove item: ${e.message}"
                Log.e("CartViewModel", "Failed to remove item from cart", e)
            }
        }
    }

    fun updateItemQuantity(itemId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeItemFromCart(itemId)
            return
        }

        viewModelScope.launch {
            try {
                val currentCart = _currentCart.value ?: return@launch
                val userId = auth.currentUser?.uid ?: return@launch

                val updatedItems = currentCart.items.map { item ->
                    if (item.id == itemId) {
                        item.copyItem(quantity = newQuantity)
                    } else {
                        item
                    }
                }

                val updatedCart = currentCart.copy(
                    items = updatedItems,
                    totalPrice = calculateTotalPrice(updatedItems)
                )

                cartRepository.update(userId.hashCode().toLong(), updatedCart)
                _currentCart.value = updatedCart

                Log.d("CartViewModel", "Updated item quantity: $itemId to $newQuantity")
            } catch (e: Exception) {
                _error.value = "Failed to update quantity: ${e.message}"
                Log.e("CartViewModel", "Failed to update item quantity", e)
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            try {
                val currentCart = _currentCart.value ?: return@launch
                val userId = auth.currentUser?.uid ?: return@launch

                val emptyCart = currentCart.copy(
                    items = emptyList(),
                    totalPrice = 0
                )

                cartRepository.update(userId.hashCode().toLong(), emptyCart)
                _currentCart.value = emptyCart
                _qrCodeBitmap.value = null

                Log.d("CartViewModel", "Cart cleared")
            } catch (e: Exception) {
                _error.value = "Failed to clear cart: ${e.message}"
                Log.e("CartViewModel", "Failed to clear cart", e)
            }
        }
    }

    fun generatePaymentQRCode() {
        viewModelScope.launch {
            try {
                val currentCart = _currentCart.value ?: return@launch
                val userId = auth.currentUser?.uid ?: return@launch

                if (currentCart.items.isEmpty()) {
                    _error.value = "Cart is empty"
                    return@launch
                }

                // Check if items are still available before generating QR
                if (!checkItemsAvailability()) {
                    _error.value = "Some items are no longer available. Please review your cart."
                    return@launch
                }

                // Check if buyer has enough caps
                val buyerProfile = profileRepository.read(userId)
                if (buyerProfile == null) {
                    _error.value = "Failed to load profile"
                    return@launch
                }

                if (buyerProfile.caps < currentCart.totalPrice) {
                    _error.value = "Insufficient caps. You need ${currentCart.totalPrice} caps but have ${buyerProfile.caps}"
                    return@launch
                }

                // Create payment data with user ID, cart items, and total
                val paymentData = buildString {
                    append("PAYMENT:")
                    append("BUYER_ID=$userId,")
                    append("SELLER_ID=${currentCart.sellerId},")
                    append("TOTAL=${currentCart.totalPrice},")
                    append("ITEMS=${currentCart.items.size},")
                    append("TIMESTAMP=${System.currentTimeMillis()}")
                }

                // Generate QR code
                val qrCodeWriter = QRCodeWriter()
                val bitMatrix = qrCodeWriter.encode(
                    paymentData,
                    BarcodeFormat.QR_CODE,
                    512,
                    512
                )

                val width = bitMatrix.width
                val height = bitMatrix.height
                val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap[x, y] =
                            if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    }
                }

                _qrCodeBitmap.value = bitmap
                Log.d("CartViewModel", "QR code generated for payment: $paymentData")

            } catch (e: Exception) {
                _error.value = "Failed to generate QR code: ${e.message}"
                Log.e("CartViewModel", "Failed to generate QR code", e)
            }
        }
    }

    fun dismissQRCode() {
        _qrCodeBitmap.value = null
    }

    fun dismissError() {
        _error.value = null
    }

    /**
     * Check if all items in cart are still available from seller
     * This should be called before generating QR code
     */
    suspend fun checkItemsAvailability(): Boolean {
        val currentCart = _currentCart.value ?: return false
        val sellerId = currentCart.sellerId ?: return false

        return try {
            // For each item in cart, check if seller still has enough quantity
            currentCart.items.all { cartItem ->
                val sellerItem = sellingRepository.readFromUser(sellerId, cartItem.id)
                sellerItem != null && sellerItem.quantity >= cartItem.quantity
            }
        } catch (e: Exception) {
            Log.e("CartViewModel", "Failed to check availability", e)
            false
        }
    }

    /**
     * Complete the purchase transaction
     * This should be called when seller scans the QR code
     * 1. Verify items are still available
     * 2. Transfer items from seller to buyer
     * 3. Transfer caps from buyer to seller
     * 4. Log transaction
     * 5. Clear cart
     */
    suspend fun completePurchase(): Result<String> {
        return try {
            val currentCart = _currentCart.value
            if (currentCart == null || currentCart.items.isEmpty()) {
                return Result.failure(Exception("Cart is empty"))
            }

            val buyerId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))
            val sellerId = currentCart.sellerId
                ?: return Result.failure(Exception("No seller specified"))

            // 1. Check availability
            if (!checkItemsAvailability()) {
                return Result.failure(Exception("Some items are no longer available"))
            }

            // 2. Check if buyer has enough caps
            val buyerProfile = profileRepository.read(buyerId)
                ?: return Result.failure(Exception("Buyer profile not found"))

            if (buyerProfile.caps < currentCart.totalPrice) {
                return Result.failure(Exception("Insufficient caps. Need ${currentCart.totalPrice}, have ${buyerProfile.caps}"))
            }

            // 3. Transfer items from seller's selling to buyer's inventory
            currentCart.items.forEach { cartItem ->
                // Remove from seller's selling inventory using sellerId
                val sellerItem = sellingRepository.readFromUser(sellerId, cartItem.id)
                if (sellerItem != null) {
                    val newQuantity = sellerItem.quantity - cartItem.quantity
                    if (newQuantity > 0) {
                        // Update seller's item quantity
                        sellingRepository.updateForUser(
                            sellerId,
                            cartItem.id,
                            sellerItem.copyItem(quantity = newQuantity)
                        )
                    } else {
                        // Remove item completely if sold out
                        sellingRepository.deleteFromUser(sellerId, cartItem.id)
                    }
                }

                // Add to buyer's inventory
                val buyerItem = inventoryRepository.read(cartItem.id)
                if (buyerItem != null) {
                    // Item exists, increase quantity
                    inventoryRepository.update(
                        cartItem.id,
                        buyerItem.copyItem(quantity = buyerItem.quantity + cartItem.quantity)
                    )
                } else {
                    // New item for buyer
                    inventoryRepository.create(cartItem.toItem())
                }
            }

            // 4. Transfer caps
            profileRepository.deductCaps(buyerId, currentCart.totalPrice)
            profileRepository.addCaps(sellerId, currentCart.totalPrice)

            // 5. Log transaction
            val transaction = Transaction(
                id = System.currentTimeMillis(),
                date = Date(),
                amount = currentCart.totalPrice.toFloat(),
                userDestination = User(
                    userId = sellerId,
                    username = currentCart.sellerName ?: "Unknown Seller"
                )
            )
            transactionRepository.create(transaction)

            // 6. Clear cart
            clearCart()

            Log.d("CartViewModel", "Purchase completed: ${currentCart.items.size} items for ${currentCart.totalPrice} caps")
            Result.success("Purchase completed successfully!")

        } catch (e: Exception) {
            Log.e("CartViewModel", "Failed to complete purchase", e)
            Result.failure(e)
        }
    }

    private fun calculateTotalPrice(items: List<SellingItem>): Int {
        return items.sumOf { it.valuePerUnit * it.quantity }
    }
}

