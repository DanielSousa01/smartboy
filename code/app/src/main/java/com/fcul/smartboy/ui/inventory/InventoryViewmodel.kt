package com.fcul.smartboy.ui.inventory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.inventory.Category
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewmodel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val items: StateFlow<List<Item>> = _items.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        populateDatabase()
        observeInventory()
    }

    private fun observeInventory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                inventoryRepository.observeInventory().collect { items ->
                    _items.value = items
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val items = inventoryRepository.getInventory()

                _items.value = items
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addItem(item: Item) {
        _items.update { it + item }
    }

    fun removeItem(item: Long) {
        _items.update { list -> list.filter { it.id != item } }
        viewModelScope.launch {
            Log.d("Inventory", "Removing item with ID: $item")
            inventoryRepository.delete(item)
            Log.d("Inventory", "Item removed with ID: $item")
        }
    }

    fun changeQuantity(itemId: Long, quantity: Int) {
        val item = _items.value.find { it.id == itemId }
        if (item != null) {
            val newItem = item.copyItem(quantity = quantity)
            _items.update { list -> list.map { if (it.id == itemId) newItem else it } }
            viewModelScope.launch {
                inventoryRepository.update(itemId, newItem)
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
                    removeItem(ammo.id)
                    _items.update { list ->
                        list.map { if (it.id == itemId) newWeapon else it }
                    }
                    viewModelScope.launch {
                        inventoryRepository.update(itemId, newWeapon)
                    }

                } else {
                    val newAmmo =
                        ammo.copyItem(quantity = ammo.quantity - (weapon.ammoMax - weapon.ammoLoaded))
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

    fun populateDatabase() {
        viewModelScope.launch {
            val samples = sampleItems()

            samples.forEach { item ->
                try {
                    inventoryRepository.create(item)
                } catch (e: Exception) {
                    Log.e("InventorySeeder", "Erro ao adicionar ${item.name}: ${e.message}")
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


