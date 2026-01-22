package com.fcul.smartboy.ui.cart.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.ui.inventory.components.IncrementalTextField

@Composable
fun CartItemDetails(
    item: SellingItem,
    buyingView: Boolean,
    onRemove: () -> Unit = {},
    onDismiss: () -> Unit,
    onAddToCart: () -> Unit = {}
) {
    var quantity: Int? by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item is SellingItem.Weapon && item.ammoId != null) {
                    Card {
                        Text(text = "Ammo : ${item.ammoName}")

                        Column(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "Max Ammo: ${item.ammoMax}")
                            }
                        }
                    }
                }

                Card {
                    if (buyingView) {
                        Text(text = "Available Quantity: ${item.quantity}")

                        IncrementalTextField(
                            value = quantity?.toString() ?: "",
                            onValueChange = { newValue ->
                                quantity = newValue.toIntOrNull()
                            },
                            onIncrement = {
                                quantity?.let {
                                    if (it < item.quantity) {
                                        quantity = it + 1
                                    }
                                }
                            },
                            isIncrementEnabled = quantity != null && quantity!! < item.quantity,
                            onDecrement = {
                                quantity?.let {
                                    if (it > 1) {
                                        quantity = it - 1
                                    }
                                }
                            },
                            isDecrementEnabled = quantity != null && quantity!! > 1,
                        )
                    } else {
                        Text(text = "Quantity: ${item.quantity}")
                    }
                }

                Card {
                    Text(text = "Value per Unit: ${item.valuePerUnit}")
                    Text(text = "Total Value: ${item.quantity * item.valuePerUnit}")
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (buyingView) {
                    Button(
                        onClick = {
                            onAddToCart()
                            onDismiss()
                        },
                        enabled = quantity != null && quantity!! <= item.quantity
                    ) {
                        Text("Add to Cart")
                    }
                } else {
                    Button(
                        onClick = {
                            onRemove()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Remove")
                    }
                }
            }
        },
    )
}