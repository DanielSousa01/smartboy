package com.fcul.smartboy.ui.cart.vm

// Error keys for UI to resolve to string resources
sealed class CartError {
    object CartEmpty : CartError()
    object ItemsUnavailable : CartError()
    object ProfileLoadFailed : CartError()
    object FailedToAddItem : CartError()
    object FailedToRemoveItem : CartError()
    object FailedToUpdateQuantity : CartError()
    object FailedToClearCart : CartError()
    object FailedToGenerateQRCode : CartError()
    object FailedToCheckAvailability : CartError()
    object FailedToFetchAndAddItem : CartError()
    data class ItemNotFound(val item: String? = null) : CartError()
    data class InsufficientCaps(val required: Int, val available: Int) : CartError()
    data class ItemQuantityExceeded(val itemName: String, val available: Int) : CartError()
    data class Generic(val message: String? = null) : CartError()
}