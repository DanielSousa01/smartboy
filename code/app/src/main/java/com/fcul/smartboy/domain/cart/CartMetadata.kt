package com.fcul.smartboy.domain.cart

data class CartMetadata(
    val userId: String? = null,
    val userName: String? = null,
    val sellerId: String? = null,
    val sellerName: String? = null,
    val totalPrice: Int = 0
)
