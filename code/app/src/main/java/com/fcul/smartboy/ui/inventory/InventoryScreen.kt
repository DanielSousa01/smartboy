package com.fcul.smartboy.ui.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.fcul.smartboy.ui.inventory.components.ItemCard
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InventoryScreen(
    isLoadingState: StateFlow<Boolean>,
    itemsState: StateFlow<List<Item>>,
    onReload: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onQuantityChange: (Long, Int) -> Unit,
) {
    val items by itemsState.collectAsState()
    var isMenuOpen by remember { mutableStateOf(false) }
    val itemsByCategory: Map<Category, List<Item>> = items.groupBy { it.category }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp)
        ) {
            Category.entries.forEach { category ->
                item {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                val categoryItems = itemsByCategory[category] ?: emptyList()

                if (categoryItems.isNotEmpty()) {
                    items(categoryItems) { invItem ->
                        ItemCard(
                            item = invItem,
                            onReload = onReload,
                            onQuantityChange = onQuantityChange,
                            onRemove = onRemove
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "No items in this category",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
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
