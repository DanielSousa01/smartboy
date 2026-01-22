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
    // Map of sellerId -> Cart
    private val _carts = MutableStateFlow<Map<String, Cart>>(emptyMap())
    val carts: StateFlow<Map<String, Cart>> = _carts.asStateFlow()

    // Currently selected cart (by sellerId)
    private val _selectedSellerId = MutableStateFlow<String?>(null)
    val selectedSellerId: StateFlow<String?> = _selectedSellerId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
    val qrCodeBitmap: StateFlow<Bitmap?> = _qrCodeBitmap.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAllCarts()
    }

    private fun loadAllCarts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _isLoading.value = false
                    return@launch
                }

                cartRepository.observeCarts().collect { cartsList ->
                    val cartsMap = cartsList.associateBy { it.sellerId ?: "" }
                    _carts.value = cartsMap
                    _isLoading.value = false
                    Log.d("CartViewModel", "Loaded ${cartsMap.size} carts")
                }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load carts: ${e.message}"
                _isLoading.value = false
                Log.e("CartViewModel", "Failed to load carts", e)
            }
        }
    }

    fun selectCart(sellerId: String?) {
        _selectedSellerId.value = sellerId
    }

    fun getCurrentCart(): Cart? {
        val sellerId = _selectedSellerId.value ?: return null
        return _carts.value[sellerId]
    }

    fun getOrCreateCartForSeller(sellerId: String, sellerName: String): Cart {
        val existingCart = _carts.value[sellerId]
        if (existingCart != null) return existingCart

        // Create new cart for this seller
        val userId = auth.currentUser?.uid ?: ""
        val userName = auth.currentUser?.displayName ?: "Guest"

        return Cart(
            userId = userId,
            userName = userName,
            sellerId = sellerId,
            sellerName = sellerName,
            totalPrice = 0,
            items = emptyList()
        )
    }

    fun addItemToCart(item: SellingItem, sellerId: String, sellerName: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                // Get or create cart for this seller
                val cart = getOrCreateCartForSeller(sellerId, sellerName)

                // Check if item already exists in cart
                val existingItemIndex = cart.items.indexOfFirst { it.id == item.id }

                val updatedItems = if (existingItemIndex >= 0) {
                    // Update quantity of existing item
                    cart.items.toMutableList().apply {
                        val existingItem = this[existingItemIndex]
                        this[existingItemIndex] = existingItem.copyItem(
                            quantity = existingItem.quantity + 1
                        )
                    }
                } else {
                    // Add new item to cart
                    cart.items + item.copyItem(quantity = 1)
                }

                val updatedCart = cart.copy(
                    items = updatedItems,
                    totalPrice = calculateTotalPrice(updatedItems)
                )

                // Save to repository
                val cartId = generateCartId(userId, sellerId)
                cartRepository.update(cartId, updatedCart)

                // Update local state
                _carts.value += (sellerId to updatedCart)

                Log.d("CartViewModel", "Added item to cart for seller: $sellerName")
                Log.d("CartViewModel", "Cart ID: $cartId, UserId: $userId, SellerId: $sellerId")
            } catch (e: Exception) {
                _error.value = "Failed to add item: ${e.message}"
                Log.e("CartViewModel", "Failed to add item to cart", e)
            }
        }
    }

    private fun generateCartId(userId: String, sellerId: String): Long {
        return "$userId-$sellerId".hashCode().toLong()
    }

    fun removeItemFromCart(itemId: Long) {
        viewModelScope.launch {
            try {
                val sellerId = _selectedSellerId.value ?: return@launch
                val currentCart = _carts.value[sellerId] ?: return@launch
                val userId = auth.currentUser?.uid ?: return@launch

                val updatedItems = currentCart.items.filter { it.id != itemId }
                val updatedCart = currentCart.copy(
                    items = updatedItems,
                    totalPrice = calculateTotalPrice(updatedItems)
                )

                val cartId = generateCartId(userId, sellerId)
                cartRepository.update(cartId, updatedCart)

                _carts.value += (sellerId to updatedCart)

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
                val sellerId = _selectedSellerId.value ?: return@launch
                val currentCart = _carts.value[sellerId] ?: return@launch
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

                val cartId = generateCartId(userId, sellerId)
                cartRepository.update(cartId, updatedCart)

                _carts.value += (sellerId to updatedCart)

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
                val sellerId = _selectedSellerId.value ?: return@launch
                val currentCart = _carts.value[sellerId] ?: return@launch
                val userId = auth.currentUser?.uid ?: return@launch

                val emptyCart = currentCart.copy(
                    items = emptyList(),
                    totalPrice = 0
                )

                val cartId = generateCartId(userId, sellerId)
                cartRepository.update(cartId, emptyCart)
                _carts.value += (sellerId to emptyCart)
                _qrCodeBitmap.value = null

                Log.d("CartViewModel", "Cart cleared for seller: $sellerId")
            } catch (e: Exception) {
                _error.value = "Failed to clear cart: ${e.message}"
                Log.e("CartViewModel", "Failed to clear cart", e)
            }
        }
    }

    fun generatePaymentQRCode() {
        viewModelScope.launch {
            try {
                val sellerId = _selectedSellerId.value ?: return@launch
                val currentCart = _carts.value[sellerId] ?: return@launch
                val userId = auth.currentUser?.uid ?: return@launch

                if (currentCart.items.isEmpty()) {
                    _error.value = "Cart is empty"
                    return@launch
                }

                // Check if items are still available before generating QR
                if (!checkItemsAvailability(sellerId)) {
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

    fun getSellingItem(sellerId: String, itemId: Long) {
        viewModelScope.launch {
            try {
                // Fetch the item from the seller's selling inventory
                val item = sellingRepository.readFromUser(sellerId, itemId)

                if (item == null) {
                    _error.value = "Item not found or no longer available"
                    Log.e("CartViewModel", "Item $itemId not found from seller $sellerId")
                    return@launch
                }

                // Get seller's profile to get their name
                val sellerProfile = profileRepository.read(sellerId)
                val sellerName = sellerProfile?.username ?: "Unknown Seller"

                // Add item to this seller's cart
                addItemToCart(item, sellerId, sellerName)

                Log.d("CartViewModel", "Added item ${item.name} from seller $sellerName to cart")
            } catch (e: Exception) {
                _error.value = "Failed to add item: ${e.message}"
                Log.e("CartViewModel", "Failed to fetch and add item", e)
            }
        }
    }

    suspend fun checkItemsAvailability(sellerId: String): Boolean {
        val currentCart = _carts.value[sellerId] ?: return false

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

    suspend fun completePurchase(buyerId: String, sellerId: String): Result<String> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            // Verify that current user is the seller
            if (currentUserId != sellerId) {
                return Result.failure(Exception("You are not the seller for this transaction"))
            }
            // Fetch the buyer's cart for this seller from the BUYER'S collection
            val cartId = generateCartId(buyerId, sellerId)
            val buyerCart = cartRepository.readFromUser(buyerId, cartId)

            Log.d("CartViewModel", "Completing purchase for buyer $buyerId from seller $sellerId")
            Log.d("CartViewModel", "Generated cart ID: $cartId")
            Log.d("CartViewModel", "Buyer cart: $buyerCart")

            if (buyerCart == null || buyerCart.items.isEmpty()) {
                return Result.failure(Exception("Buyer's cart is empty or not found"))
            }

            // 1. Check availability - check the buyer's cart items against seller's inventory
            val allItemsAvailable = buyerCart.items.all { cartItem ->
                val sellerItem = sellingRepository.readFromUser(sellerId, cartItem.id)
                sellerItem != null && sellerItem.quantity >= cartItem.quantity
            }

            if (!allItemsAvailable) {
                return Result.failure(Exception("Some items are no longer available"))
            }

            // 2. Check if buyer has enough caps
            val buyerProfile = profileRepository.read(buyerId)
                ?: return Result.failure(Exception("Buyer profile not found"))

            if (buyerProfile.caps < buyerCart.totalPrice) {
                return Result.failure(Exception("Insufficient caps. Need ${buyerCart.totalPrice}, have ${buyerProfile.caps}"))
            }

            // 3. Transfer items from seller's selling to buyer's inventory
            buyerCart.items.forEach { cartItem ->
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
                    inventoryRepository.create(cartItem.toItem())
                }
            }

            // 4. Transfer caps
            profileRepository.deductCaps(buyerId, buyerCart.totalPrice)
            profileRepository.addCaps(sellerId, buyerCart.totalPrice)

            // 5. Log transaction
            val transaction = Transaction(
                id = System.currentTimeMillis(),
                date = Date(),
                amount = buyerCart.totalPrice.toFloat(),
                userDestination = User(
                    userId = sellerId,
                    username = buyerCart.sellerName ?: "Unknown Seller"
                )
            )
            transactionRepository.create(transaction)

            // 6. Clear buyer's cart in BUYER'S collection
            val emptyCart = buyerCart.copy(items = emptyList(), totalPrice = 0)
            cartRepository.updateForUser(buyerId, cartId, emptyCart)

            Log.d("CartViewModel", "Purchase completed: ${buyerCart.items.size} items for ${buyerCart.totalPrice} caps")
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

