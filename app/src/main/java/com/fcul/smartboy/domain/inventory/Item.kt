package com.fcul.smartboy.domain.inventory

data class Item(
    val id: Long,
    val name: String,
    val quantity: Int = 1,
    val category: Int
) {
    init {
        require(quantity >= 0) { "Quantity must be non-negative" }
        require(category >= 0 && category <= Category.entries.size)
        { "Category must be between 0 and ${Category.entries.size}" }
    }
}
