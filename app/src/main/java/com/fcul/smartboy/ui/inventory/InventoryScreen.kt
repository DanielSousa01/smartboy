package com.fcul.smartboy.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.inventory.Category
import com.fcul.smartboy.domain.inventory.Item

@Composable
fun InventoryScreen(
    items: List<Item> = sampleItems(),
) {

    val itemsByCategory: Map<Int, List<Item>> = items.groupBy { it.category }

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

            val categoryItems = itemsByCategory[category.ordinal] ?: emptyList()

            if (categoryItems.isNotEmpty()) {
                items(categoryItems) { invItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = invItem.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "x${invItem.quantity}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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

private fun sampleItems(): List<Item> {
    return listOf(
        Item(id = 1, name = "Sword", quantity = 1, category = Category.WEAPONS.ordinal),
        Item(id = 2, name = "Bow", quantity = 2, category = Category.WEAPONS.ordinal),
        Item(id = 3, name = "Bandage", quantity = 5, category = Category.AID.ordinal),
        Item(id = 4, name = "Health Potion", quantity = 3, category = Category.AID.ordinal),
        Item(id = 5, name = "Shield", quantity = 1, category = Category.WEAPONS.ordinal)
    )
}


@Preview(showBackground = true)
@Composable
private fun InventoryScreenPreview() {
    InventoryScreen()
}