package com.fcul.smartboy.ui.inventory.components.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.ui.inventory.components.IncrementalTextField
import com.fcul.smartboy.ui.inventory.components.selling.SellingItem as SellingItemDialog

@Composable
fun ItemDetails(
    item: Item,
    sellingItem: SellingItem? = null,
    onUse: () -> Unit,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onSellingItemQuantityChange: (Int) -> Unit,
    onSellingItemValueChange: (Int) -> Unit,
    onSell: (Int, Int) -> Unit,
) {
    var isSellingMenuOpen by remember { mutableStateOf(false) }
    var quantity by remember { mutableIntStateOf(item.quantity) }
    var sellingQuantity by remember { mutableIntStateOf(sellingItem?.quantity ?: 0) }
    var sellingValue by remember { mutableIntStateOf(sellingItem?.valuePerUnit ?: 0) }
    var hasChanges by remember { mutableStateOf(false) }

    if (isSellingMenuOpen) {
        SellingItemDialog(
            item = item,
            onDismiss = { isSellingMenuOpen = false },
            onConfirm = { qty, price ->
                onSell(qty, price)
                isSellingMenuOpen = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (sellingItem == null) {
                    OutlinedButton(
                        onClick = { isSellingMenuOpen = true },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sell,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Sell")
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Weapon ammo info
                if (item is Item.Weapon && item.ammoId != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ammo: ${item.ammoName}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.ammoLoaded ?: 0} / ${item.ammoMax ?: 0}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if ((item.ammoLoaded ?: 0) < (item.ammoMax ?: 0)) {
                                Button(onClick = onReload) {
                                    Text("Reload")
                                }
                            }
                        }
                    }
                }

                // Selling information (if listed for sale)
                if (sellingItem != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Currently Listed for Sale",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Selling quantity
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Selling Quantity",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                IncrementalTextField(
                                    value = sellingQuantity.toString(),
                                    onValueChange = { newValue ->
                                        newValue.toIntOrNull()?.let {
                                            val maxSellable = item.quantity - (sellingItem.quantity - sellingQuantity)
                                            if (it in 1..maxSellable) {
                                                sellingQuantity = it
                                                hasChanges = true
                                            }
                                        }
                                    },
                                    onIncrement = {
                                        val maxSellable = item.quantity - (sellingItem.quantity - sellingQuantity)
                                        if (sellingQuantity < maxSellable) {
                                            sellingQuantity++
                                            hasChanges = true
                                        }
                                    },
                                    isIncrementEnabled = sellingQuantity < item.quantity,
                                    onDecrement = {
                                        if (sellingQuantity > 1) {
                                            sellingQuantity--
                                            hasChanges = true
                                        }
                                    },
                                    isDecrementEnabled = sellingQuantity > 1
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Selling price
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Price per Unit (Caps)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                IncrementalTextField(
                                    value = sellingValue.toString(),
                                    onValueChange = { newValue ->
                                        newValue.toIntOrNull()?.let {
                                            if (it >= 0) {
                                                sellingValue = it
                                                hasChanges = true
                                            }
                                        }
                                    },
                                    onIncrement = {
                                        sellingValue++
                                        hasChanges = true
                                    },
                                    isIncrementEnabled = true,
                                    onDecrement = {
                                        if (sellingValue > 0) {
                                            sellingValue--
                                            hasChanges = true
                                        }
                                    },
                                    isDecrementEnabled = sellingValue > 0
                                )
                            }
                        }
                    }
                }

                // Inventory quantity
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Inventory Quantity",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        IncrementalTextField(
                            value = quantity.toString(),
                            onValueChange = { newValue ->
                                newValue.toIntOrNull()?.let {
                                    if (it >= 1) {
                                        quantity = it
                                        hasChanges = true
                                    }
                                }
                            },
                            onIncrement = {
                                quantity++
                                hasChanges = true
                            },
                            isIncrementEnabled = true,
                            onDecrement = {
                                if (quantity > 1) {
                                    quantity--
                                    hasChanges = true
                                }
                            },
                            isDecrementEnabled = quantity > 1
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Delete button
                OutlinedButton(
                    onClick = {
                        onRemove()
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }

                // Update button (if changes made)
                if (hasChanges) {
                    Button(
                        onClick = {
                            // Update inventory quantity
                            if (quantity != item.quantity) {
                                onQuantityChange(quantity)
                            }

                            // Update selling info
                            if (sellingItem != null) {
                                if (sellingQuantity != sellingItem.quantity) {
                                    onSellingItemQuantityChange(sellingQuantity)
                                }
                                if (sellingValue != sellingItem.valuePerUnit) {
                                    onSellingItemValueChange(sellingValue)
                                }
                            }

                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Changes")
                    }
                } else {
                    // Close button when no changes
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                }

                // Use button (for weapons and aid items)
                if ((item is Item.Weapon && item.ammoId != null && (item.ammoLoaded ?: 0) > 0) ||
                    (item is Item.Aid && item.quantity > 0)) {
                    Button(
                        onClick = {
                            onUse()
                            if (item is Item.Aid && quantity > 0) {
                                quantity--
                                if (quantity == 0) {
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Use")
                    }
                }
            }
        }
    )
}

