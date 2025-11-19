package com.fcul.smartboy.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.inventory.Category
import com.fcul.smartboy.domain.inventory.Item
import kotlinx.coroutines.flow.StateFlow

@Composable
fun InventoryScreen(
    itemsState: StateFlow<List<Item>>,
    onReload: (Long) -> Unit = {}
) {
    val items by itemsState.collectAsState()

    val itemsByCategory: Map<Category, List<Item>> = items.groupBy { it.category }

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
                    ItemEntry(
                        item = invItem,
                        onReload = onReload
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
}

@Composable
private fun ItemEntry(
    item: Item,
    onReload: (Long) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            if (item is Item.Weapon && item.ammoId != null) {
                Column {
                    if (item.ammoName != null) {
                        Text(
                            text = "Ammo Name:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = item.ammoName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                Column {
                    Button(
                        onClick = {
                            onReload(item.id)
                        }
                    ) {
                        Text(
                            text = "Reload",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (item is Item.Weapon && item.ammoId != null && item.ammoMax != null)
                    Text(
                        text = "Ammo: ${item.ammoLoaded}/${item.ammoMax}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                Text(
                    text = "Quantity: ${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

        }
    }
}

