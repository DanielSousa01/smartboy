package com.fcul.smartboy.domain.inventory

sealed class SellingItem {
    abstract val id: Long
    abstract val name: String
    abstract val quantity: Int
    abstract val category: Category
    abstract val valuePerUnit: Int

    init {
        require(quantity >= 0) { "Quantity must be non-negative" }
        require(valuePerUnit >= 0) { "Value must be non-negative"}
    }

    abstract fun copyItem(
        id: Long = this.id,
        name: String = this.name,
        quantity: Int = this.quantity,
        category: Category = this.category,
    ): SellingItem

    data class Weapon(
        override val id: Long,
        override val name: String,
        override val quantity: Int,
        override val category: Category,
        override val valuePerUnit: Int,
        val ammoId: Long? = null,
        val ammoName: String? = null,
        val ammoMax: Int? = null,
        val ammoLoaded: Int? = null,
    ) : SellingItem() {
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
        ): SellingItem {
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
        ): SellingItem {
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
        override val category: Category,
        override val valuePerUnit: Int,
    ) : SellingItem() {
        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category
        ): SellingItem {
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
        override val category: Category,
        override val valuePerUnit: Int,
    ) : SellingItem() {
        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category
        ): SellingItem {
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
        override val category: Category,
        override val valuePerUnit: Int,
    ) : SellingItem() {
        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category
        ): SellingItem {
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
        override val category: Category,
        override val valuePerUnit: Int,
    ) : SellingItem() {
        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category
        ): SellingItem {
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
        override val category: Category,
        override val valuePerUnit: Int,
    ) : SellingItem() {
        override fun copyItem(
            id: Long,
            name: String,
            quantity: Int,
            category: Category
        ): SellingItem {
            return this.copy(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )
        }
    }

    fun toEntity(): SellingItemEntity {
        return SellingItemEntity(
            id = this.id,
            name = this.name,
            quantity = this.quantity,
            category = this.category.name,
            ammoId = if (this is Weapon) this.ammoId else null,
            ammoName = if (this is Weapon) this.ammoName else null,
            ammoMax = if (this is Weapon) this.ammoMax else null,
            ammoLoaded = if (this is Weapon) this.ammoLoaded else null,
            valuePerUnit = this.valuePerUnit
        )
    }

    fun toItem(): Item {
        return when (this) {
            is Weapon -> {
                Item.Weapon(
                    id = this.id,
                    name = this.name,
                    quantity = this.quantity,
                    category = this.category,
                    ammoId = this.ammoId,
                    ammoName = this.ammoName,
                    ammoMax = this.ammoMax,
                    ammoLoaded = this.ammoLoaded
                )
            }
            is Ammo -> {
                Item.Ammo(
                    id = this.id,
                    name = this.name,
                    quantity = this.quantity,
                    category = this.category
                )
            }
            is Apparel -> {
                Item.Apparel(
                    id = this.id,
                    name = this.name,
                    quantity = this.quantity,
                    category = this.category
                )
            }
            is Aid -> {
                Item.Aid(
                    id = this.id,
                    name = this.name,
                    quantity = this.quantity,
                    category = this.category
                )
            }
            is Misc -> {
                Item.Misc(
                    id = this.id,
                    name = this.name,
                    quantity = this.quantity,
                    category = this.category
                )
            }
            is Junk -> {
                Item.Junk(
                    id = this.id,
                    name = this.name,
                    quantity = this.quantity,
                    category = this.category
                )

            }
        }
    }
}
