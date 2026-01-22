package com.fcul.smartboy.utils

import com.fcul.smartboy.domain.inventory.Category
import com.fcul.smartboy.domain.inventory.Item

/**
 * Sample items to populate new user inventories
 */
object SampleItems {
    fun getStarterItems(): List<Item> {
        return listOf(
            Item.Weapon(id = 1, name = "Knife", quantity = 1, category = Category.WEAPONS),
            Item.Weapon(
                id = 2,
                name = "Pistol",
                quantity = 1,
                category = Category.WEAPONS,
                ammoId = 5,
                ammoName = "10mm",
                ammoMax = 7,
                ammoLoaded = 0
            ),
            Item.Aid(id = 3, name = "Stimpak", quantity = 5, category = Category.AID),
            Item.Aid(id = 4, name = "Psycho", quantity = 1, category = Category.AID),
            Item.Ammo(id = 5, name = "10mm", quantity = 40, category = Category.AMMO),
            Item.Aid(id = 6, name = "RadAway", quantity = 3, category = Category.AID),
            Item.Aid(id = 7, name = "Rad-X", quantity = 2, category = Category.AID),
        )
    }
}
