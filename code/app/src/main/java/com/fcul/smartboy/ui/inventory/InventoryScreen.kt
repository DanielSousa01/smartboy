package com.fcul.smartboy.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
    val isLoading by isLoadingState.collectAsState()
    var isMenuOpen by remember { mutableStateOf(false) }
    var isRemoveMenuOpen by remember { mutableStateOf(false) }
    var isItemQuantityMenuOpen by remember { mutableStateOf(false) }

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
                        ItemEntry(
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
                FloatingActionButtonMenuItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Remove Item") },
                    text = { Text("Remove Item") },
                    onClick = {
                        isRemoveMenuOpen = true
                        isMenuOpen = false
                    }
                )
                FloatingActionButtonMenuItem(
                    icon = {
                        Icon(
                            Icons.Default.ChangeCircle,
                            contentDescription = "Change Item Quantity"
                        )
                    },
                    text = { Text("Change Item Quantity") },
                    onClick = {
                        isItemQuantityMenuOpen = true
                        isMenuOpen = false
                    }
                )
            }
        }
    }
    when {
        isRemoveMenuOpen -> RemoveItemMenu(
            items = items,
            onDismiss = { isRemoveMenuOpen = false },
            onRemove = onRemove
        )

        isItemQuantityMenuOpen -> ItemQuantityMenu(
            items = items,
            onDismiss = { isItemQuantityMenuOpen = false },
            onQuantityChange = onQuantityChange
        )

        else -> {}
    }
}

@Composable
private fun ItemEntry(
    item: Item,
    onReload: (Long) -> Unit,
    onQuantityChange: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
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

            if (item is Item.Aid) {
                Column {
                    Button(
                        onClick = {
                            if (item.quantity - 1 == 0)
                                onRemove(item.id)
                            else
                                onQuantityChange(item.id, item.quantity - 1)
                        }
                    ) {
                        Text("Use")
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

@Composable
private fun RemoveItemMenu(
    items: List<Item>,
    onDismiss: () -> Unit,
    onRemove: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove item") },
        text = {
            if (items.isEmpty()) {
                Text("No items to remove")
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedItem?.name ?: "Select an item to remove",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f)
                        )
                        IconButton(
                            onClick = {
                                expanded = !expanded
                            }
                        ) {
                            Icon(
                                imageVector = if (!expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = "Expand"
                            )
                        }
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name) },
                            onClick = {
                                selectedItem = item
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRemove(selectedItem?.id!!)
                    onDismiss()
                },
                enabled = selectedItem != null
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ItemQuantityMenu(
    items: List<Item>,
    onDismiss: () -> Unit,
    onQuantityChange: (Long, Int) -> Unit
) {
    var quantity by remember { mutableStateOf(1) }
    var expanded by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Item Quantity") },
        text = {
            if (items.isEmpty()) {
                Text("No items to change quantity for")
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedItem?.name ?: "Select an item to change quantity for",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f)
                        )
                        IconButton(
                            onClick = {
                                expanded = !expanded
                            }
                        ) {
                            Icon(
                                imageVector = if (!expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = "Expand"
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    quantity++
                                },
                                enabled = selectedItem != null
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Quantity"
                                )
                            }
                            TextField(
                                value = quantity.toString(),
                                onValueChange = {
                                    if (it.toIntOrNull() != null && it.toInt() > 0) {
                                        quantity = it.toInt()
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .width(64.dp)
                                    .padding(vertical = 4.dp),
                                enabled = selectedItem != null
                            )
                            IconButton(
                                onClick = {
                                    quantity--
                                },
                                enabled = selectedItem != null && quantity > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Increase Quantity",
                                )
                            }
                        }
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name) },
                            onClick = {
                                selectedItem = item
                                quantity = item.quantity
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onQuantityChange(selectedItem?.id!!, quantity)
                    onDismiss()
                },
                enabled = selectedItem != null
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}





