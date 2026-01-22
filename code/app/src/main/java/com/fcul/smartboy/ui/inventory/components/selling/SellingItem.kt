package com.fcul.smartboy.ui.inventory.components.selling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.ui.inventory.components.IncrementalTextField

@Composable
fun SellingItem(
    item: Item,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var quantity by remember { mutableIntStateOf(1) }
    var pricePerUnit by remember { mutableIntStateOf(10) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Sell,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Sell Item",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Available quantity info
                Text(
                    text = "Available: ${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quantity selector
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
                            text = "Quantity to Sell",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        IncrementalTextField(
                            value = quantity.toString(),
                            onValueChange = { newValue ->
                                newValue.toIntOrNull()?.let {
                                    if (it in 1..item.quantity) {
                                        quantity = it
                                    }
                                }
                            },
                            onIncrement = {
                                if (quantity < item.quantity) {
                                    quantity++
                                }
                            },
                            isIncrementEnabled = quantity < item.quantity,
                            onDecrement = {
                                if (quantity > 1) {
                                    quantity--
                                }
                            },
                            isDecrementEnabled = quantity > 1
                        )
                    }
                }

                // Price selector
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
                                    }
                                }
                            },
                            onIncrement = { pricePerUnit++ },
                            isIncrementEnabled = true,
                            onDecrement = {
                                if (pricePerUnit > 0) {
                                    pricePerUnit--
                                }
                            },
                            isDecrementEnabled = pricePerUnit > 0
                        )
                    }
                }

                // Total display
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
                        Text(
                            text = "${quantity * pricePerUnit} Caps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(quantity, pricePerUnit) },
                enabled = quantity > 0 && quantity <= item.quantity && pricePerUnit >= 0
            ) {
                Icon(
                    imageVector = Icons.Default.Sell,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("List for Sale")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

