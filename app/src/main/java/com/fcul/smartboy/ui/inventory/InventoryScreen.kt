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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.inventory.Category
import com.fcul.smartboy.domain.inventory.Item

@Composable
fun InventoryScreen(
    items: List<Item> = sampleItems(),
) {

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
                    ItemEntry(invItem)
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
private fun ItemEntry(item: Item) {
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
                        onClick = { item.ammoLoaded = item.ammoLoaded?.plus(item.ammoMax ?: 0) }
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


private fun sampleItems(): List<Item> {
    return listOf(
        Item.Weapon(id = 1, name = "Sword", quantity = 1, category = Category.WEAPONS),
        Item.Weapon(
            id = 2,
            name = "Bow",
            quantity = 2,
            category = Category.WEAPONS,
            ammoId = 6,
            ammoName = "Arrow",
            ammoMax = 1,
            ammoLoaded = 0
        ),
        Item.Aid(id = 3, name = "Bandage", quantity = 5, category = Category.AID),
        Item.Aid(id = 4, name = "Health Potion", quantity = 3, category = Category.AID),
        Item.Weapon(id = 5, name = "Shield", quantity = 1, category = Category.WEAPONS),
        Item.Ammo(id = 6, name = "Arrow", quantity = 10, category = Category.AMMO),

        )
}


@Preview(showBackground = true)
@Composable
private fun InventoryScreenPreview() {
    InventoryScreen()
}

