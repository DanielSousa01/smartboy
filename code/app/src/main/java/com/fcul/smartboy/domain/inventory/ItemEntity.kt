package com.fcul.smartboy.domain.inventory

data class ItemEntity(
    val id: Long = 0,
    val name: String = "",
    val quantity: Int = 0,
    val category: String = "",
    val ammoId: Long? = null,
    val ammoName: String? = null,
    val ammoMax: Int? = null,
    val ammoLoaded: Int? = null
) {
    fun toItem(): Item {
        val category = Category.valueOf(category)

        return when (category) {
            Category.WEAPONS -> Item.Weapon(
                id = id,
                name = name,
                quantity = quantity,
                category = category,
                ammoId = ammoId,
                ammoName = ammoName,
                ammoMax = ammoMax,
                ammoLoaded = ammoLoaded
            )
            Category.AMMO -> Item.Ammo(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )
            Category.APPAREL -> Item.Apparel(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )
            Category.AID -> Item.Aid(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )

            Category.MISC -> Item.Misc(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )
            Category.JUNK -> Item.Junk(
                id = id,
                name = name,
                quantity = quantity,
                category = category
            )

        }
    }
}

