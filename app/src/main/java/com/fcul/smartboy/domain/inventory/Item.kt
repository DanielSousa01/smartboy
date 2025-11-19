package com.fcul.smartboy.domain.inventory

sealed class Item {
    abstract val id: Long
    abstract val name: String
    abstract var quantity: Int
    abstract val category: Category

    init {
        require(quantity >= 0) { "Quantity must be non-negative" }
    }

    data class Weapon(
        override val id: Long,
        override val name: String,
        override var quantity: Int,
        override val category: Category,
        val ammoId: Long? = null,
        val ammoName: String? = null,
        val ammoMax: Int? = null,
        var ammoLoaded: Int? = null
    ) : Item() {
        init {
            if (ammoId != null) {
                require(ammoName != null) { "ammoName must be provided when ammoId is not null" }
                require(ammoMax != null) { "ammoMax must be provided when ammoId is not null" }
                require(ammoLoaded != null) { "ammoLoaded must be provided when ammoId is not null" }
            }
        }
    }

    data class Ammo(
        override val id: Long,
        override val name: String,
        override var quantity: Int,
        override val category: Category
    ) : Item()

    data class Apparel(
        override val id: Long,
        override val name: String,
        override var quantity: Int,
        override val category: Category
    ) : Item()

    data class Aid(
        override val id: Long,
        override val name: String,
        override var quantity: Int,
        override val category: Category
    ) : Item()

    data class Misc(
        override val id: Long,
        override val name: String,
        override var quantity: Int,
        override val category: Category
    ) : Item()

    data class Junk(
        override val id: Long,
        override val name: String,
        override var quantity: Int,
        override val category: Category
    ) : Item()
}
