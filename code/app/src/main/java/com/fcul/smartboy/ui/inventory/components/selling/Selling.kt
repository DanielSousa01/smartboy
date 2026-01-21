package com.fcul.smartboy.ui.inventory.components.selling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.inventory.Category
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.domain.inventory.SellingItem

@Composable
fun Selling(
    inventoryItems: List<Item>,
    sellingItems: List<SellingItem>,
    userId: String?,
    onRemove: (Long) -> Unit,
    onQuantityChange: (Long, Int) -> Unit,
    onValueChange: (Long, Int) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    val filteredItems = if (selectedCategory != null) {
        sellingItems.filter { it.category == selectedCategory }
    } else {
        sellingItems
    }

    val categories = sellingItems.map { it.category }.distinct()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") }
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.displayName) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
        ) {
            items(filteredItems) { sellingItem ->
                SellingItemCard(
                    inventoryItem = inventoryItems.find { it.id == sellingItem.id },
                    item = sellingItem,
                    userId = userId,
                    onRemove = {
                        onRemove(sellingItem.id)
                    },
                    onQuantityChange = { newQuantity ->
                        onQuantityChange(sellingItem.id, newQuantity)
                    },
                    onValueChange = { newValue ->
                        onValueChange(sellingItem.id, newValue)
                    }
                )
            }
        }
    }
}