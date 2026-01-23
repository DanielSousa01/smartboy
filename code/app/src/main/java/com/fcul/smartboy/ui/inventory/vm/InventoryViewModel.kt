package com.fcul.smartboy.ui.inventory.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.repository.InventoryRepository
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.repository.SellingRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val sellingRepository: SellingRepository,
    private val profileRepository: ProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    private val _sellingItems = MutableStateFlow<List<SellingItem>>(emptyList())
    val sellingItems: StateFlow<List<SellingItem>> = _sellingItems.asStateFlow()

    private val _error = MutableStateFlow<InventoryError?>(null)
    val error: StateFlow<InventoryError?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeInventory()
        observeSellingItems()
    }

    fun onDismissError() {
        _error.value = null
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
                _error.value = InventoryError.FailedToObserveInventory
                Log.e(TAG, "Error observing inventory: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }

    private fun observeSellingItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sellingRepository.observeSellingItems().collect { items ->
                    _sellingItems.value = items
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = InventoryError.FailedToObserveSellingItems
                Log.e(TAG, "Error observing selling items: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }

    fun addItem(item: Item) {
        _items.update { it + item }
        viewModelScope.launch {
            try {
                inventoryRepository.create(item)
            } catch (e: Exception) {
                _error.value = InventoryError.FailedToAddItem
                Log.e(TAG, "Error adding item: ${e.message}", e)
            }
        }
    }

    fun removeItem(item: Long) {
        _items.update { list -> list.filter { it.id != item } }
        viewModelScope.launch {
            try {
                inventoryRepository.delete(item)
            } catch (e: Exception) {
                _error.value = InventoryError.FailedToRemoveItem
                Log.e(TAG, "Error removing item: ${e.message}", e)
            }
        }
    }

    fun removeSellingItem(item: Long) {
        val removedSellingItem = _sellingItems.value.find { it.id == item }

        if (removedSellingItem != null) {
            _sellingItems.value.filter { it.id != item }

            viewModelScope.launch {
                sellingRepository.delete(item)
            }

            val inventoryItem = _items.value.find { it.id == removedSellingItem.id }

            if (inventoryItem != null) {
                val newQuantity = inventoryItem.quantity + removedSellingItem.quantity

                changeItemQuantity(removedSellingItem.id, newQuantity)
            } else {
                addItem(removedSellingItem.toItem())
            }
        }
    }


    fun changeItemQuantity(itemId: Long, quantity: Int) {
        val item = _items.value.find { it.id == itemId }
        if (item != null) {
            val newItem = item.copyItem(quantity = quantity)
            _items.update { list -> list.map { if (it.id == itemId) newItem else it } }
            viewModelScope.launch {
                try {
                    inventoryRepository.update(itemId, newItem)
                } catch (e: Exception) {
                    _error.value = InventoryError.FailedToUpdateItem
                    Log.e(TAG, "Error updating item quantity: ${e.message}", e)
                }
            }
        }
    }

    fun changeSellingItemQuantity(itemId: Long, quantity: Int) {
        val sellingItem = _sellingItems.value.find { it.id == itemId }
        val item = _items.value.find { it.id == sellingItem?.id }

        if (sellingItem != null) {
            val quantityDiff = sellingItem.quantity - quantity

            val newSellingItem = sellingItem.copyItem(quantity = quantity)

            _sellingItems.update { list -> list.map { if (it.id == itemId) newSellingItem else it } }

            if (item != null) {
                if (item.quantity + quantityDiff <= 0) {
                    removeItem(itemId)
                } else {
                    changeItemQuantity(itemId, item.quantity + quantityDiff)
                }
            } else if (quantityDiff > 0) {
                addItem(newSellingItem.toItem())
            }

            viewModelScope.launch {
                sellingRepository.update(itemId, newSellingItem)
            }
        }
    }

    fun changeSellingItemValue(itemId: Long, value: Int) {
        val sellingItem = _sellingItems.value.find { it.id == itemId }

        if (sellingItem != null) {
            val newSellingItem = sellingItem.copyItem(valuePerUnit = value)

            _sellingItems.update { list -> list.map { if (it.id == itemId) newSellingItem else it } }

            viewModelScope.launch {
                sellingRepository.update(itemId, newSellingItem)
            }
        }
    }

    fun unloadAmmo(itemId: Long) {
        val weapon = _items.value.find { it.id == itemId }

        if (weapon is Item.Weapon && weapon.ammoId != null
            && weapon.ammoMax != null && weapon.ammoLoaded != null && weapon.ammoLoaded > 0
        ) {
            val newWeapon = weapon.copyItem(ammoLoaded = weapon.ammoLoaded - 1)

            _items.update { list ->
                list.map {
                    if (it.id == itemId) newWeapon else it
                }
            }

            viewModelScope.launch {
                inventoryRepository.update(itemId, newWeapon)
            }
        }
    }

    fun reloadAmmo(itemId: Long) {
        val weapon = _items.value.find { it.id == itemId }

        if (weapon is Item.Weapon && weapon.ammoId != null
            && weapon.ammoMax != null && weapon.ammoLoaded != null
            && weapon.ammoLoaded < weapon.ammoMax
        ) {
            Log.d(TAG, "Reloading ammo for weapon with ID: ${weapon.name}")

            val ammo = _items.value.find { it.id == weapon.ammoId }

            if (ammo is Item.Ammo) {
                val ammoNeeded = weapon.ammoMax - weapon.ammoLoaded
                if (ammo.quantity >= ammoNeeded) {
                    // Enough ammo to fully reload
                    val newWeapon = weapon.copyItem(ammoLoaded = weapon.ammoMax)
                    val newAmmo = ammo.copyItem(quantity = ammo.quantity - ammoNeeded)

                    _items.update { list ->
                        list.map {
                            when (it.id) {
                                weapon.id -> newWeapon
                                ammo.id -> newAmmo
                                else -> it
                            }
                        }.filterNot { it is Item.Ammo && it.quantity == 0 }
                    }
                    viewModelScope.launch {
                        inventoryRepository.update(itemId, newWeapon)
                        if (newAmmo.quantity > 0) {
                            inventoryRepository.update(ammo.id, newAmmo)
                        } else {
                            inventoryRepository.delete(ammo.id)
                        }
                    }
                } else {
                    // Not enough ammo to fully reload
                    val newWeapon = weapon.copyItem(ammoLoaded = weapon.ammoLoaded + ammo.quantity)
                    _items.update { list ->
                        list.map {
                            if (it.id == weapon.id) newWeapon else it
                        }.filter { it.id != ammo.id }
                    }
                    viewModelScope.launch {
                        inventoryRepository.update(itemId, newWeapon)
                        inventoryRepository.delete(ammo.id)
                    }
                }
            }
        }
    }

    fun sellItem(itemId: Long, quantity: Int, value: Int) {
        val item = _items.value.find { it.id == itemId }
        if (item != null) {
            val newSellingItem = item.toSellingItem(quantity, value)

            if (item.quantity - quantity <= 0) {
                removeItem(itemId)
            } else {
                changeItemQuantity(itemId, item.quantity - quantity)
            }
            viewModelScope.launch {
                try {
                    sellingRepository.create(newSellingItem)
                } catch (e: Exception) {
                    _error.value = InventoryError.FailedToSellItem
                    Log.e(TAG, "Error selling item: ${e.message}", e)
                }
            }
        }
    }

    fun useItem(itemId: Long): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val item = _items.value.find { it.id == itemId }

        if (item == null) {
            Log.w(TAG, "Cannot use item: not found")
            _error.value = InventoryError.ItemNotFound
            return false
        }

        if (item.quantity <= 0) {
            Log.w(TAG, "Cannot use item, quantity is 0")
            _error.value = InventoryError.QuantityZero
            return false
        }

        viewModelScope.launch {
            try {
                when (item.name) {
                    "RadAway" -> {
                        // RadAway reduces current radiation by 50 Sv
                        val success = profileRepository.useRadAway(userId, RADAWAY_REDUCTION)
                        if (success) {
                            changeItemQuantity(itemId, item.quantity - 1)
                            Log.i(
                                TAG,
                                "RadAway used: reduced radiation by $RADAWAY_REDUCTION Sv"
                            )
                        } else {
                            Log.w(
                                TAG,
                                "RadAway use failed: no radiation to remove"
                            )
                        }
                    }

                    "Rad-X" -> {
                        // Rad-X provides 50% radiation resistance for 5 minutes
                        val success = profileRepository.useRadX(
                            userId,
                            RADX_RESISTANCE_BOOST,
                            RADX_DURATION_MS
                        )
                        if (success) {
                            changeItemQuantity(itemId, item.quantity - 1)
                            Log.i(
                                TAG,
                                "Rad-X used: increased resistance by ${RADX_RESISTANCE_BOOST * 100}%"
                            )

                            // Schedule resistance decay after duration
                            viewModelScope.launch {
                                delay(RADX_DURATION_MS)
                                profileRepository.decreaseRadiationResistance(
                                    userId,
                                    RADX_RESISTANCE_BOOST
                                )
                                Log.i(TAG, "Rad-X effect expired")
                            }
                        }
                    }

                    else -> {
                        Log.w(TAG, "Item ${item.name} has no use effect")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to use item: ${e.message}", e)
                _error.value = InventoryError.ItemFailToUse
            }
        }
        return true
    }

    companion object {
        private const val RADAWAY_REDUCTION = 50.0 // Reduces 50 Sv of radiation
        private const val RADX_RESISTANCE_BOOST = 0.5 // 50% radiation resistance
        private const val RADX_DURATION_MS = 300000L // 5 minutes
        private const val TAG = "InventoryViewmodel"
    }
}