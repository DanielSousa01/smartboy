package com.fcul.smartboy.domain.cart

import com.fcul.smartboy.domain.inventory.SellingItem

data class Cart(
    val userId: String? = null,
    val userName: String? = null,
    val sellerId: String? = null,
    val sellerName: String? = null,
    val totalPrice: Int = 0,
    val items: List<SellingItem> = emptyList()
)
