package com.fcul.smartboy.ui.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fcul.smartboy.domain.inventory.Category
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.ui.inventory.components.inventory.Inventory
import com.fcul.smartboy.ui.inventory.components.selling.Selling
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InventoryScreen(
    isLoadingState: StateFlow<Boolean>,
    itemsState: StateFlow<List<Item>>,
    sellingItemsState: StateFlow<List<SellingItem>>,
    onUnload: (Long) -> Unit,
    onReload: (Long) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onRemoveSellingItem: (Long) -> Unit,
    onItemQuantityChange: (Long, Int) -> Unit,
    onSellingItemQuantityChange: (Long, Int) -> Unit,
    onSellingItemValueChange: (Long, Int) -> Unit,
    onSell: (Long, Int, Int) -> Unit
) {
    val items by itemsState.collectAsState()
    val sellingItems by sellingItemsState.collectAsState()

    val itemsByCategory: Map<Category, List<Item>> = items.groupBy { it.category }
    var selectedTab by remember { mutableStateOf(0) }


    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Inventory") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Selling") }
                )
            }

            when (selectedTab) {
                0 -> {
                    Inventory(
                        itemsByCategory = itemsByCategory,
                        onUnload = onUnload,
                        onReload = onReload,
                        onRemove = onRemoveItem,
                        onQuantityChange = onItemQuantityChange,
                        onSell = onSell
                    )
                }

                1 -> {
                    Selling(
                        inventoryItems = items,
                        sellingItems = sellingItems,
                        onRemove = onRemoveSellingItem,
                        onQuantityChange = onSellingItemQuantityChange,
                        onValueChange = onSellingItemValueChange
                    )
                }
            }
        }
    }
}
