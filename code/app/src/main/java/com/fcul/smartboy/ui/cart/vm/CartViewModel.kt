package com.fcul.smartboy.ui.cart.vm

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
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

@HiltViewModel
class CartViewModel @Inject constructor(
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

    private val _error = MutableStateFlow<CartError?>(null)
    val error: StateFlow<CartError?> = _error.asStateFlow()

    // Track last processed transaction to prevent duplicates
    private var lastProcessedTransaction: Pair<String, String>? = null
    private var lastProcessedTimestamp: Long = 0

    init {
        loadAllCarts()
    }

    fun onDismissError() {
        _error.value = null
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
                    Log.d(TAG, "Loaded ${cartsMap.size} carts")
                }
                _error.value = null
            } catch (e: Exception) {
                _error.value = CartError.Generic("Failed to load carts: ${e.message}")
                _isLoading.value = false
                Log.e(TAG, "Failed to load carts", e)
            }
        }
    }

    fun selectCart(sellerId: String?) {
        _selectedSellerId.value = sellerId
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

                val currentCartQuantity = if (existingItemIndex >= 0) {
                    cart.items[existingItemIndex].quantity
                } else {
                    0
                }

                val newTotalQuantity = currentCartQuantity + 1

                // Validate against available quantity
                if (newTotalQuantity > item.quantity) {
                    _error.value = CartError.ItemQuantityExceeded(item.name, item.quantity)
                    Log.w(TAG, "Attempted to add more than available: $newTotalQuantity > ${item.quantity}")
                    return@launch
                }

                val updatedItems = if (existingItemIndex >= 0) {
                    // Update quantity of existing item
                    cart.items.toMutableList().apply {
                        val existingItem = this[existingItemIndex]
                        this[existingItemIndex] = existingItem.copyItem(
                            quantity = newTotalQuantity
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

                Log.d(TAG, "Added item to cart for seller: $sellerName (${newTotalQuantity}/${item.quantity})")
                Log.d(TAG, "Cart ID: $cartId, UserId: $userId, SellerId: $sellerId")
            } catch (e: Exception) {
                _error.value = CartError.FailedToAddItem
                Log.e(TAG, "Failed to add item to cart", e)
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

                Log.d(TAG, "Removed item from cart: $itemId")
            } catch (e: Exception) {
                _error.value = CartError.FailedToRemoveItem
                Log.e(TAG, "Failed to remove item from cart", e)
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

                // Find the item being updated
                val cartItem = currentCart.items.find { it.id == itemId }
                if (cartItem == null) {
                    _error.value = CartError.ItemNotFound()
                    return@launch
                }

                // Get the seller's current inventory for this item
                val sellerItem = try {
                    sellingRepository.readFromUser(sellerId, itemId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get seller item", e)
                    null
                }

                // Validate sellerItem is not null and quantity is within bounds
                val sellerAvailable = sellerItem?.quantity ?: 0
                if (sellerItem == null) {
                    _error.value = CartError.ItemsUnavailable
                    return@launch
                }
                if (newQuantity > sellerAvailable) {
                    _error.value = CartError.ItemQuantityExceeded(cartItem.name, sellerAvailable)
                    Log.w(TAG, "Quantity validation failed: $newQuantity > $sellerAvailable")
                    return@launch
                }

                // Validation passed, update the quantity
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

                Log.d(TAG, "Updated item quantity: $itemId to $newQuantity (max: $sellerAvailable)")
            } catch (e: Exception) {
                _error.value = CartError.FailedToUpdateQuantity
                Log.e(TAG, "Failed to update item quantity", e)
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

                Log.d(TAG, "Cart cleared for seller: $sellerId")
            } catch (e: Exception) {
                _error.value = CartError.FailedToClearCart
                Log.e(TAG, "Failed to clear cart", e)
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
                    _error.value = CartError.CartEmpty
                    return@launch
                }

                // Check if items are still available before generating QR
                if (!checkItemsAvailability(sellerId)) {
                    _error.value = CartError.ItemsUnavailable
                    return@launch
                }

                // Check if buyer has enough caps
                val buyerProfile = profileRepository.read(userId)
                if (buyerProfile == null) {
                    _error.value = CartError.ProfileLoadFailed
                    return@launch
                }

                if (buyerProfile.caps < currentCart.totalPrice) {
                    _error.value = CartError.InsufficientCaps(currentCart.totalPrice, buyerProfile.caps)
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
                    QR_CODE_SIZE,
                    QR_CODE_SIZE
                )

                val width = bitMatrix.width
                val height = bitMatrix.height
                val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    }
                }

                _qrCodeBitmap.value = bitmap
                Log.d(TAG, "QR code generated for payment: $paymentData")

            } catch (e: Exception) {
                _error.value = CartError.FailedToGenerateQRCode
                Log.e(TAG, "Failed to generate QR code", e)
            }
        }
    }

    fun dismissQRCode() {
        _qrCodeBitmap.value = null
    }

    fun getSellingItem(sellerId: String, itemId: Long) {
        viewModelScope.launch {
            try {
                // Fetch the item from the seller's selling inventory
                val item = sellingRepository.readFromUser(sellerId, itemId)

                if (item == null) {
                    _error.value = CartError.ItemsUnavailable
                    Log.e(TAG, "Item $itemId not found from seller $sellerId")
                    return@launch
                }

                // Get seller's profile to get their name
                val sellerProfile = profileRepository.read(sellerId)
                val sellerName = sellerProfile?.username ?: "Unknown Seller"

                // Add item to this seller's cart
                addItemToCart(item, sellerId, sellerName)

                Log.d(TAG, "Added item ${item.name} from seller $sellerName to cart")
            } catch (e: Exception) {
                _error.value = CartError.FailedToFetchAndAddItem
                Log.e(TAG, "Failed to fetch and add item", e)
            }
        }
    }

    suspend fun checkItemsAvailability(sellerId: String): Boolean {
        val currentCart = _carts.value[sellerId] ?: return false

        return try {
            // For each item in cart, check if seller still has enough quantity
            val enough = currentCart.items.all { cartItem ->
                val sellerItem = sellingRepository.readFromUser(sellerId, cartItem.id)
                sellerItem != null && sellerItem.quantity >= cartItem.quantity
            }
            if (!enough) {
                Log.w(TAG, "Some items in cart are no longer available from seller $sellerId")
                _error.value = CartError.ItemsUnavailable
                false
            } else {
                true
            }
        } catch (e: Exception) {
            _error.value = CartError.FailedToCheckAvailability
            Log.e(TAG, "Failed to check availability", e)
            false
        }
    }

    suspend fun completePurchase(buyerId: String, sellerId: String): Result<String> {
        return try {
            // Prevent duplicate processing - check if this exact transaction was just processed
            val currentTime = System.currentTimeMillis()
            val transactionKey = Pair(buyerId, sellerId)

            if (lastProcessedTransaction == transactionKey &&
                (currentTime - lastProcessedTimestamp) < TRANSACTION_PROCESSING_WINDOW_MS) { // 3 second window
                Log.w(TAG, "Duplicate transaction detected, ignoring")
                return Result.failure(Exception("Transaction already being processed"))
            }

            // Mark this transaction as being processed
            lastProcessedTransaction = transactionKey
            lastProcessedTimestamp = currentTime

            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            // Verify that current user is the seller
            if (currentUserId != sellerId) {
                return Result.failure(Exception("You are not the seller for this transaction"))
            }
            // Fetch the buyer's cart for this seller from the BUYER'S collection
            val cartId = generateCartId(buyerId, sellerId)
            val buyerCart = cartRepository.readFromUser(buyerId, cartId)

            Log.d(TAG, "Completing purchase for buyer $buyerId from seller $sellerId")
            Log.d(TAG, "Generated cart ID: $cartId")
            Log.d(TAG, "Buyer cart: $buyerCart")

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

                // Add to BUYER's inventory (not seller's!)
                val buyerItem = inventoryRepository.readFromUser(buyerId, cartItem.id)
                if (buyerItem != null) {
                    // Item exists in buyer's inventory, increase quantity
                    inventoryRepository.updateForUser(
                        buyerId,
                        cartItem.id,
                        buyerItem.copyItem(quantity = buyerItem.quantity + cartItem.quantity)
                    )
                } else {
                    // New item for buyer, create it
                    inventoryRepository.createForUser(buyerId, cartItem.toItem())
                }
            }

            // 4. Transfer caps
            profileRepository.deductCaps(buyerId, buyerCart.totalPrice)
            profileRepository.addCaps(sellerId, buyerCart.totalPrice)

            // 5. Log transactions for both buyer and seller
            val timestamp = System.currentTimeMillis()

            // Create transaction for BUYER (negative - they paid)
            val buyerTransaction = Transaction(
                id = timestamp,
                date = Date(),
                amount = -buyerCart.totalPrice.toFloat(), // Negative for buyer
                userDestination = User(
                    userId = sellerId,
                    username = buyerCart.sellerName ?: "Unknown Seller"
                )
            )

            // Create transaction for SELLER (positive - they received)
            val sellerTransaction = Transaction(
                id = timestamp,
                date = Date(),
                amount = buyerCart.totalPrice.toFloat(), // Positive for seller
                userDestination = User(
                    userId = buyerId,
                    username = buyerCart.userName ?: "Unknown Buyer"
                )
            )

            // Save both transactions to their respective collections
            transactionRepository.createForUser(buyerId, buyerTransaction)
            transactionRepository.createForUser(sellerId, sellerTransaction)

            // 6. Delete buyer's cart from BUYER'S collection
            cartRepository.deleteForUser(buyerId, cartId)

            Log.d(TAG, "Purchase completed: ${buyerCart.items.size} items for ${buyerCart.totalPrice} caps")
            Result.success("Purchase completed successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete purchase", e)
            Result.failure(e)
        }
    }

    private fun calculateTotalPrice(items: List<SellingItem>): Int {
        return items.sumOf { it.valuePerUnit * it.quantity }
    }

    companion object {
        private const val QR_CODE_SIZE = 512
        private const val TRANSACTION_PROCESSING_WINDOW_MS = 3000L
        private const val TAG = "CartViewModel"
    }
}