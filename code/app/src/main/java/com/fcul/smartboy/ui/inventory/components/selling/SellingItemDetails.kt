package com.fcul.smartboy.ui.inventory.components.selling

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.ui.inventory.components.IncrementalTextField


@Composable
fun SellingItemDetails(
    inventoryItem: Item?,
    item: SellingItem,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onValueChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    var quantity: Int? by remember { mutableStateOf(item.quantity) }
    var inventoryQuantity: Int? by remember { mutableStateOf(inventoryItem?.quantity) }
    var value: Int? by remember { mutableStateOf(item.valuePerUnit) }

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

                Button(onClick = {
                }) {
                    Text("Sell")
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
                    Text(text = "Quantity: ")
                    IncrementalTextField(
                        value = quantity?.toString() ?: "",
                        onValueChange = { newValue ->
                            quantity = newValue.toIntOrNull()
                        },
                        onIncrement = {
                            quantity?.let {
                                if (inventoryQuantity != null && inventoryQuantity!! > 0) {
                                    quantity = it + 1
                                    inventoryQuantity = inventoryQuantity!! - 1
                                }
                            }
                        },
                        isIncrementEnabled = quantity != null && inventoryQuantity != null
                                && inventoryQuantity!! > 0,
                        onDecrement = {
                            quantity?.let {
                                if (it > 1) {
                                    quantity = it - 1
                                    inventoryQuantity = if (inventoryQuantity != null) {
                                        inventoryQuantity!! + 1
                                    } else {
                                        1
                                    }
                                }
                            }
                        },
                        isDecrementEnabled = quantity != null && quantity!! > 1,
                    )
                }

                Card {
                    Text(text = "Value per Unit: ")
                    IncrementalTextField(
                        value = value?.toString() ?: "",
                        onValueChange = { newValue ->
                            value = newValue.toIntOrNull()
                        },
                        onIncrement = {
                            value?.let {
                                value = it + 1
                            }
                        },
                        isIncrementEnabled = value != null,
                        onDecrement = {
                            value?.let {
                                if (it > 0) {
                                    value = it - 1
                                }
                            }
                        },
                        isDecrementEnabled = value != null && value!! > 0,
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
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

                Button(
                    onClick = {
                        var dismissible = true

                        value?.let {
                            if (it != item.valuePerUnit && it > 0) {
                                onValueChange(it)
                            } else if (it < 0) {
                                dismissible = false
                                Toast.makeText(
                                    context,
                                    "Value must be greater or equal to 0",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        quantity?.let {
                            if (it != item.quantity && it > 0
                                && inventoryQuantity != null && inventoryQuantity!! > 0) {
                                onQuantityChange(it)
                            } else if (it == 0) {
                                dismissible = false
                                Toast.makeText(
                                    context,
                                    "Quantity must be greater than 0",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        if (dismissible)
                            onDismiss()

                    },
                    enabled = quantity != null && item.quantity != quantity!! && quantity!! > 0 ||
                            value != null && item.valuePerUnit != value!! && value!! >= 0
                ) {
                    Text("Update")
                }
            }
        },
    )
}