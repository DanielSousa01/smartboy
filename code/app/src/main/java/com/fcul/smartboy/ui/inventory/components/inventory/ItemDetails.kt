package com.fcul.smartboy.ui.inventory.components.inventory

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
import com.fcul.smartboy.ui.inventory.components.selling.SellingItem


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
    val context = LocalContext.current
    var isSellingMenuOpen by remember { mutableStateOf(false) }
    var quantity: Int? by remember { mutableStateOf(item.quantity) }
    var sellingQuantity: Int? by remember { mutableStateOf(sellingItem?.quantity) }
    var sellingValue: Int? by remember { mutableStateOf(sellingItem?.valuePerUnit) }
    var tempItemQuantity by remember { mutableStateOf(item.quantity) }

    if (isSellingMenuOpen)
        SellingItem(
            item = item,
            onDismiss = { isSellingMenuOpen = false },
            onConfirm = { quantity, price ->
                onSell(quantity, price)
                isSellingMenuOpen = false
            },
        )

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

                if (sellingItem == null) {
                    Button(onClick = {
                        isSellingMenuOpen = true
                    }) {
                        Text("Sell")
                    }
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
                if (sellingItem != null) {
                    Card {
                        Column(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(text = "Selling:")
                            }

                            IncrementalTextField(
                                value = sellingQuantity?.toString() ?: "",
                                onValueChange = { newValue ->
                                    sellingQuantity = newValue.toIntOrNull()
                                },
                                onIncrement = {
                                    sellingQuantity?.let {
                                        if (tempItemQuantity > 0) {
                                            sellingQuantity = it + 1
                                            tempItemQuantity--
                                        }
                                    }
                                },
                                isIncrementEnabled = sellingQuantity != null && tempItemQuantity > 0,
                                onDecrement = {
                                    sellingQuantity?.let {
                                        if (it > 0) {
                                            sellingQuantity = it - 1
                                            tempItemQuantity++
                                        }
                                    }
                                },
                                isDecrementEnabled = sellingQuantity != null && sellingQuantity!! > 0
                            )

                            Row(
                                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(text = "Value per Unit:")
                            }

                            IncrementalTextField(
                                value = sellingValue?.toString() ?: "",
                                onValueChange = { newValue ->
                                    sellingValue = newValue.toIntOrNull()
                                },
                                onIncrement = {
                                    sellingValue?.let {
                                        sellingValue = it + 1
                                    }
                                },
                                isIncrementEnabled = sellingValue != null,
                                onDecrement = {
                                    sellingValue?.let {
                                        if (it > 0) {
                                            sellingValue = it - 1
                                        }
                                    }
                                },
                                isDecrementEnabled = sellingValue != null && sellingValue!! > 0
                            )
                        }
                    }
                }
                if (item is Item.Weapon && item.ammoId != null) {
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
                                Text(text = "${item.ammoLoaded} / ${item.ammoMax}")
                            }
                            Button(onClick = onReload) {
                                Text("Reload")
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
                                quantity = it + 1
                            }
                        },
                        isIncrementEnabled = quantity != null,
                        onDecrement = {
                            quantity?.let {
                                if (it > 1) {
                                    quantity = it - 1
                                }
                            }
                        },
                        isDecrementEnabled = quantity != null && quantity!! > 1,
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

                        quantity?.let {
                            if (it != item.quantity && it > 0) {
                                onQuantityChange(it)
                            } else if (it != quantity!! && it < 0) {
                                dismissible = false
                                Toast.makeText(
                                    context,
                                    "Quantity must be greater than 0",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        sellingValue?.let {
                            if (sellingItem != null && it != sellingItem.valuePerUnit && it > 0) {
                                onSellingItemValueChange(it)
                            } else if (sellingItem != null && sellingItem.valuePerUnit != sellingValue!!
                                && sellingValue!! < 0) {
                                dismissible = false

                                Toast.makeText(
                                    context,
                                    "Value must be greater or equal to 0"
                                    , Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        sellingQuantity?.let {
                            if (sellingItem != null && it != sellingItem.quantity && it > 0 && tempItemQuantity >= 0) {
                                onSellingItemQuantityChange(it)
                            } else if (sellingItem != null && sellingItem.quantity != sellingQuantity!!
                                && sellingQuantity!! < 0 && tempItemQuantity >= 0) {
                                dismissible = false

                                Toast.makeText(
                                    context,
                                    "Quantity must be greater than 0"
                                    , Toast.LENGTH_SHORT
                                ).show()
                            } else if (sellingItem != null && sellingItem.quantity != sellingQuantity!!
                                && tempItemQuantity < 0) {
                                dismissible = false

                                Toast.makeText(
                                    context,
                                    "You don't have enough items to sell",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        if (dismissible)
                            onDismiss()
                    },
                    enabled = quantity != null && item.quantity != quantity!! && quantity!! > 0 ||
                    sellingValue != null && sellingItem != null
                            && sellingItem.valuePerUnit != sellingValue!! && sellingValue!! >= 0 ||
                    sellingQuantity != null && sellingItem != null && sellingItem.quantity != sellingQuantity!!
                            && sellingQuantity!! > 0 && tempItemQuantity >= 0
                ) {
                    Text("Update")
                }
                if (item is Item.Weapon && item.ammoId != null && item.ammoLoaded != null || item is Item.Aid)
                    Button(
                        onClick = {
                            if (item is Item.Aid && item.quantity > 0) {
                                quantity = item.quantity - 1
                                if (quantity == 0)
                                    onDismiss()
                            }
                            onUse()
                        },
                        enabled = item is Item.Weapon && item.ammoId != null && item.ammoLoaded != null
                                && item.ammoLoaded > 0 || item is Item.Aid && item.quantity > 0
                    ) {
                        Text("Use")
                    }
            }
        },
    )
}