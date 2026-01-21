package com.fcul.smartboy.ui.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    onRemove: (Long) -> Unit,
    onQuantityChange: (Long, Int) -> Unit,
    onSell: (Long, Int, Int) -> Unit
) {
    val items by itemsState.collectAsState()
    val sellingItems by sellingItemsState.collectAsState()

    var isMenuOpen by remember { mutableStateOf(false) }
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
                        onRemove = onRemove,
                        onQuantityChange = onQuantityChange,
                        onSell = onSell
                    )
                }

                1 -> {
                    Selling(
                        inventoryItems = items,
                        sellingItems = sellingItems
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButtonMenu(
                expanded = isMenuOpen,
                button = {
                    FloatingActionButton(
                        onClick = { isMenuOpen = !isMenuOpen }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Menu")
                    }
                }
            ) {
            }
        }
    }
}
