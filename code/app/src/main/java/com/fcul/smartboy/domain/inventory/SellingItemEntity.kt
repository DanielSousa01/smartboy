package com.fcul.smartboy.domain.inventory

data class SellingItemEntity(
    val id: Long = 0,
    val name: String = "",
    val quantity: Int = 0,
    val category: String = "",
    val ammoId: Long? = null,
    val ammoName: String? = null,
    val ammoMax: Int? = null,
    val ammoLoaded: Int? = null,
    val valuePerUnit: Int = 0
) {
    fun toSellingItem(): SellingItem {
        return when (val category = Category.valueOf(category)) {
            Category.WEAPONS -> SellingItem.Weapon(
                id = id,
                name = name,
                quantity = quantity,
                category = category,
                valuePerUnit = valuePerUnit,
                ammoId = ammoId,
                ammoName = ammoName,
                ammoMax = ammoMax,
                ammoLoaded = ammoLoaded
            )

            Category.AMMO -> SellingItem.Ammo(
                id = id,
                name = name,
                quantity = quantity,
                category = category,
                valuePerUnit = valuePerUnit
            )

            Category.APPAREL -> SellingItem.Apparel(
                id = id,
                name = name,
                quantity = quantity,
                category = category,
                valuePerUnit = valuePerUnit
            )

            Category.AID -> SellingItem.Aid(
                id = id,
                name = name,
                quantity = quantity,
                category = category,
                valuePerUnit = valuePerUnit
            )

            Category.MISC -> SellingItem.Misc(
                id = id,
                name = name,
                quantity = quantity,
                category = category,
                valuePerUnit = valuePerUnit
            )

            Category.JUNK -> SellingItem.Junk(
                id = id,
                name = name,
                quantity = quantity,
                category = category,
                valuePerUnit = valuePerUnit
            )

        }
    }
}

