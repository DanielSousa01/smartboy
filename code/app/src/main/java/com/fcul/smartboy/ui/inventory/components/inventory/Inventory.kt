package com.fcul.smartboy.ui.inventory.components.inventory

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.inventory.Category
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.domain.inventory.SellingItem

@Composable
fun Inventory(
    itemsByCategory: Map<Category, List<Item>>,
    sellingItems: List<SellingItem>,
    onUnload: (Long) -> Unit,
    onReload: (Long) -> Unit,
    onUseItem: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onQuantityChange: (Long, Int) -> Unit,
    onSellingItemQuantityChange: (Long, Int) -> Unit,
    onSellingItemValueChange: (Long, Int) -> Unit,
    onSell: (Long, Int, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
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
                    if (invItem is Item.Weapon && invItem.ammoId != null) {
                        ItemCard(
                            item = invItem,
                            sellingItem = sellingItems.find { it.id == invItem.id },
                            onUse = onUnload,
                            onReload = onReload,
                            onQuantityChange = onQuantityChange,
                            onSellingItemQuantityChange = onSellingItemQuantityChange,
                            onSellingItemValueChange = onSellingItemValueChange,
                            onRemove = onRemove,
                            onSell = onSell
                        )
                    } else if (invItem is Item.Aid) {
                        ItemCard(
                            item = invItem,
                            sellingItem = sellingItems.find { it.id == invItem.id },
                            onUse = {
                                val newQuantity = invItem.quantity - 1
                                if (newQuantity > 0)
                                    onQuantityChange(invItem.id, invItem.quantity - 1)
                                else
                                    onRemove(invItem.id)
                            },
                            onQuantityChange = onQuantityChange,
                            onSellingItemQuantityChange = onSellingItemQuantityChange,
                            onSellingItemValueChange = onSellingItemValueChange,
                            onRemove = onRemove,
                            onSell = onSell
                        )
                    } else {
                        ItemCard(
                            item = invItem,
                            sellingItem = sellingItems.find { it.id == invItem.id },
                            onQuantityChange = onQuantityChange,
                            onSellingItemQuantityChange = onSellingItemQuantityChange,
                            onSellingItemValueChange = onSellingItemValueChange,
                            onRemove = onRemove,
                            onSell = onSell
                        )
                    }

                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.no_items_in_category),
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
}