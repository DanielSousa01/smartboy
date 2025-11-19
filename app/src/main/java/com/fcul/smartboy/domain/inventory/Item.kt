package com.fcul.smartboy.domain.inventory

sealed class Item {
    abstract val id: Long
    abstract val name: String
    abstract val quantity: Int
    abstract val category: Category

    init {
        require(quantity >= 0) { "Quantity must be non-negative" }
    }

    abstract fun copyItem(
        id: Long = this.id,
        name: String = this.name,
        quantity: Int = this.quantity,
        category: Category = this.category,
    ): Item

    data class Weapon(
        override val id: Long,
        override val name: String,
        override val quantity: Int,
        override val category: Category,
        val ammoId: Long? = null,
        val ammoName: String? = null,
        val ammoMax: Int? = null,
        val ammoLoaded: Int? = null
    ) : Item() {
        init {
            if (ammoId != null) {
                require(ammoName != null) { "ammoName must be provided when ammoId is not null" }
                require(ammoMax != null) { "ammoMax must be provided when ammoId is not null" }
                require(ammoLoaded != null) { "ammoLoaded must be provided when ammoId is not null" }
            }
        }

        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category,
        ): Item {
            return this.copy(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )
        }

        fun copyItem(
            id: Long = this.id,
            name: String = this.name,
            quantity: Int = this.quantity,
            category: Category = this.category,
            ammoId: Long? = this.ammoId,
            ammoName: String? = this.ammoName,
            ammoMax: Int? = this.ammoMax,
            ammoLoaded: Int? = this.ammoLoaded
        ): Item {
            return this.copy(
                id = id,
                name = name,
                quantity = quantity,
                category = category,
                ammoId = ammoId,
                ammoName = ammoName,
                ammoMax = ammoMax,
                ammoLoaded = ammoLoaded
            )
        }
    }

    data class Ammo(
        override val id: Long,
        override val name: String,
        override val quantity: Int,
        override val category: Category
    ) : Item() {
        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category
        ): Item {
            return this.copy(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )
        }
    }

    data class Apparel(
        override val id: Long,
        override val name: String,
        override val quantity: Int,
        override val category: Category
    ) : Item() {
        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category
        ): Item {
            return this.copy(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )
        }
    }

    data class Aid(
        override val id: Long,
        override val name: String,
        override val quantity: Int,
        override val category: Category
    ) : Item() {
        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category
        ): Item {
            return this.copy(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )
        }
    }

    data class Misc(
        override val id: Long,
        override val name: String,
        override val quantity: Int,
        override val category: Category
    ) : Item() {
        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category
        ): Item {
            return this.copy(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )
        }
    }

    data class Junk(
        override val id: Long,
        override val name: String,
        override val quantity: Int,
        override val category: Category
    ) : Item() {
        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category
        ): Item {
            return this.copy(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )
        }
    }
}
