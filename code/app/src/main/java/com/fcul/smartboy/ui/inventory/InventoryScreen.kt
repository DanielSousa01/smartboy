package com.fcul.smartboy.ui.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.inventory.Category
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.ui.common.ErrorSnackbar
import com.fcul.smartboy.ui.inventory.components.inventory.Inventory
import com.fcul.smartboy.ui.inventory.components.selling.Selling
import com.fcul.smartboy.ui.inventory.vm.InventoryError
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InventoryScreen(
    items: List<Item>,
    sellingItems: List<SellingItem>,
    userId: String?,
    error: InventoryError?,
    onUnload: (Long) -> Unit,
    onReload: (Long) -> Unit,
    onUseItem: (Long) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onRemoveSellingItem: (Long) -> Unit,
    onItemQuantityChange: (Long, Int) -> Unit,
    onSellingItemQuantityChange: (Long, Int) -> Unit,
    onSellingItemValueChange: (Long, Int) -> Unit,
    onSell: (Long, Int, Int) -> Unit,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current

    val errorMessage = when (error) {
        is InventoryError.ItemNotFound -> context.getString(R.string.error_inventory_item_not_found)
        is InventoryError.QuantityZero -> context.getString(R.string.error_inventory_quantity_zero)
        is InventoryError.ItemFailToUse -> context.getString(R.string.error_inventory_item_fail_to_use)
        is InventoryError.FailedToObserveInventory -> context.getString(R.string.error_inventory_failed_to_observe_inventory)
        is InventoryError.FailedToObserveSellingItems -> context.getString(R.string.error_inventory_failed_to_observe_selling_items)
        is InventoryError.FailedToAddItem -> context.getString(R.string.error_inventory_failed_to_add_item)
        is InventoryError.FailedToRemoveItem -> context.getString(R.string.error_inventory_failed_to_remove_item)
        is InventoryError.FailedToUpdateItem -> context.getString(R.string.error_inventory_failed_to_update_item)
        is InventoryError.FailedToSellItem -> context.getString(R.string.error_inventory_failed_to_sell_item)
        is InventoryError.Generic -> context.getString(R.string.error_inventory_generic)
        else -> null
    }

    val itemsByCategory: Map<Category, List<Item>> = items.groupBy { it.category }
    var selectedTab by remember { mutableStateOf(InventoryTab.INVENTORY) }

    Scaffold (
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column {
            SecondaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == InventoryTab.INVENTORY,
                    onClick = { selectedTab = InventoryTab.INVENTORY },
                    text = { Text(stringResource(R.string.inventory)) }
                )
                Tab(
                    selected = selectedTab == InventoryTab.SELLING,
                    onClick = { selectedTab = InventoryTab.SELLING },
                    text = { Text(stringResource(R.string.selling)) }
                )
            }

            when (selectedTab) {
                InventoryTab.INVENTORY -> {
                    Inventory(
                        itemsByCategory = itemsByCategory,
                        sellingItems = sellingItems,
                        onUnload = onUnload,
                        onReload = onReload,
                        onUseItem = onUseItem,
                        onRemove = onRemoveItem,
                        onQuantityChange = onItemQuantityChange,
                        onSellingItemQuantityChange = onSellingItemQuantityChange,
                        onSellingItemValueChange = onSellingItemValueChange,
                        onSell = onSell
                    )
                }

                InventoryTab.SELLING -> {
                    Selling(
                        inventoryItems = items,
                        sellingItems = sellingItems,
                        userId = userId,
                        onRemove = onRemoveSellingItem,
                        onQuantityChange = onSellingItemQuantityChange,
                        onValueChange = onSellingItemValueChange
                    )
                }
            }
        }

        ErrorSnackbar(
            errorMessage = errorMessage,
            onDismissError = onDismissError
        )
    }
}

enum class InventoryTab {
    INVENTORY,
    SELLING
}
