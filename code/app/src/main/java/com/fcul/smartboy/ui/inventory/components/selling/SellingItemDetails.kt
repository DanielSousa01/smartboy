package com.fcul.smartboy.ui.inventory.components.selling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.ui.inventory.components.IncrementalTextField


@Composable
fun SellingItemDetails(
    inventoryItem: Item?,
    item: SellingItem,
    userId: String?,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onValueChange: (Int) -> Unit,
) {
    var quantity by remember { mutableIntStateOf(item.quantity) }
    var pricePerUnit by remember { mutableIntStateOf(item.valuePerUnit) }
    var hasChanges by remember { mutableStateOf(false) }
    var isQrDialogOpen by remember { mutableStateOf(false) }

    val maxAvailable = (inventoryItem?.quantity ?: 0) + item.quantity

    if (isQrDialogOpen && userId != null) {
        SellingId(
            itemId = item.id,
            itemName = item.name,
            userId = userId,
            onDismiss = { isQrDialogOpen = false }
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

                if (userId != null) {
                    OutlinedButton(
                        onClick = { isQrDialogOpen = true },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("QR")
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
                // Available inventory info
                if (inventoryItem != null) {
                    Text(
                        text = "Available in inventory: ${inventoryItem.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Weapon ammo info
                if (item is SellingItem.Weapon && item.ammoId != null) {
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
                                text = "Weapon Details",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ammo),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Ammo: ${item.ammoName}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = "Max Capacity: ${item.ammoMax}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Quantity for sale
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
                            text = "Quantity for Sale",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        IncrementalTextField(
                            value = quantity.toString(),
                            onValueChange = { newValue ->
                                newValue.toIntOrNull()?.let {
                                    if (it in 1..maxAvailable) {
                                        quantity = it
                                        hasChanges = true
                                    }
                                }
                            },
                            onIncrement = {
                                if (quantity < maxAvailable) {
                                    quantity++
                                    hasChanges = true
                                }
                            },
                            isIncrementEnabled = quantity < maxAvailable,
                            onDecrement = {
                                if (quantity > 1) {
                                    quantity--
                                    hasChanges = true
                                }
                            },
                            isDecrementEnabled = quantity > 1
                        )
                        Text(
                            text = "Max available: $maxAvailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Price per unit
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
                            text = "Price per Unit (Caps)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        IncrementalTextField(
                            value = pricePerUnit.toString(),
                            onValueChange = { newValue ->
                                newValue.toIntOrNull()?.let {
                                    if (it >= 0) {
                                        pricePerUnit = it
                                        hasChanges = true
                                    }
                                }
                            },
                            onIncrement = {
                                pricePerUnit++
                                hasChanges = true
                            },
                            isIncrementEnabled = true,
                            onDecrement = {
                                if (pricePerUnit > 0) {
                                    pricePerUnit--
                                    hasChanges = true
                                }
                            },
                            isDecrementEnabled = pricePerUnit > 0
                        )
                    }
                }

                // Total value display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Value:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${quantity * pricePerUnit}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.capicon),
                                contentDescription = stringResource(R.string.caps),
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Remove from sale button
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
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }

                // Update button (only show if changes made)
                if (hasChanges) {
                    Button(
                        onClick = {
                            if (quantity != item.quantity) {
                                onQuantityChange(quantity)
                            }
                            if (pricePerUnit != item.valuePerUnit) {
                                onValueChange(pricePerUnit)
                            }
                            onDismiss()
                        },
                        enabled = quantity in 1..maxAvailable && pricePerUnit >= 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
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
            }
        }
    )
}

