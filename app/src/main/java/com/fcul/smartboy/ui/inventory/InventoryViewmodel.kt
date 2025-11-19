package com.fcul.smartboy.ui.inventory

import android.util.Log
import androidx.lifecycle.ViewModel
import com.fcul.smartboy.domain.inventory.Category
import com.fcul.smartboy.domain.inventory.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InventoryViewmodel : ViewModel() {
    private val _items = MutableStateFlow<List<Item>>(sampleItems())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    fun addItem(item: Item) {
        _items.update { it + item }
    }

    fun removeItem(item: Item) {
        _items.update { it - item }
    }

    fun increaseQuantity(itemId: Long, quantity: Int) {
        val item = _items.value.find { it.id == itemId }
        if (item != null) {
            val newItem = item.copyItem(quantity = item.quantity + quantity)
            _items.update { list -> list.map { if (it.id == itemId) newItem else it } }
        }
    }

    fun decreaseQuantity(itemId: Long, quantity: Int) {
        val item = _items.value.find { it.id == itemId }
        if (item != null) {
            if (item.quantity <= quantity) {
                _items.update { list -> list.filter { it.id != itemId } }
            } else {
                val newItem = item.copyItem(quantity = item.quantity - quantity)
                _items.update { list -> list.map { if (it.id == itemId) newItem else it } }
            }
        }
    }

    fun reloadAmmo(itemId: Long) {
        val weapon = _items.value.find { it.id == itemId }

        if (weapon is Item.Weapon && weapon.ammoId != null
            && weapon.ammoMax != null && weapon.ammoLoaded != null
            && weapon.ammoLoaded < weapon.ammoMax
        ) {
            Log.d("InventoryViewmodel", "Reloading ammo for weapon with ID: ${weapon.name}")

            val ammo = _items.value.find { it.id == weapon.ammoId }

            if (ammo is Item.Ammo) {
                if (ammo.quantity <= weapon.ammoMax - weapon.ammoLoaded) {
                    val newWeapon = weapon.copyItem(ammoLoaded = weapon.ammoLoaded + ammo.quantity)

                    _items.update { list ->
                        list.filter { it.id != ammo.id }
                            .map { if (it.id == itemId) newWeapon else it }
                    }
                } else {
                    val newAmmo = ammo.copyItem(quantity = ammo.quantity - (weapon.ammoMax - weapon.ammoLoaded))
                    val newWeapon = weapon.copyItem(ammoLoaded = weapon.ammoMax)

                    _items.update { list ->
                        list.map {
                            when (it.id) {
                                ammo.id -> newAmmo
                                itemId -> newWeapon
                                else -> it
                            }
                        }
                    }
                }
            }

        }
    }
}

private fun sampleItems(): List<Item> {
    return listOf(
        Item.Weapon(id = 1, name = "Sword", quantity = 1, category = Category.WEAPONS),
        Item.Weapon(
            id = 2,
            name = "Bow",
            quantity = 1,
            category = Category.WEAPONS,
            ammoId = 6,
            ammoName = "Arrow",
            ammoMax = 1,
            ammoLoaded = 0
        ),
        Item.Aid(id = 3, name = "Bandage", quantity = 5, category = Category.AID),
        Item.Aid(id = 4, name = "Health Potion", quantity = 3, category = Category.AID),
        Item.Weapon(id = 5, name = "Shield", quantity = 1, category = Category.WEAPONS),
        Item.Ammo(id = 6, name = "Arrow", quantity = 1, category = Category.AMMO),

        )
}