package com.fcul.smartboy.ui.inventory.vm

sealed class InventoryError {
    object ItemNotFound : InventoryError()
    object QuantityZero : InventoryError()
    object ItemFailToUse : InventoryError()
    object FailedToObserveInventory : InventoryError()
    object FailedToObserveSellingItems : InventoryError()
    object FailedToAddItem : InventoryError()
    object FailedToRemoveItem : InventoryError()
    object FailedToUpdateItem : InventoryError()
    object FailedToSellItem : InventoryError()
    object Generic : InventoryError()
}