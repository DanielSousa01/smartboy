package com.fcul.smartboy.ui.inventory.components.selling

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.ui.inventory.components.IncrementalTextField

@Composable
fun SellingItem(
    item: Item,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var quantity: Int? by remember { mutableStateOf(1) }
    var price: Int? by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row {
                Text(text = "Selling Item")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Quantity:")
                    IncrementalTextField(
                        value = quantity?.toString() ?: "",
                        onValueChange = { newValue ->
                            quantity = newValue.toIntOrNull()
                        },
                        isIncrementEnabled = quantity != null && quantity!! < item.quantity,
                        onIncrement = {
                            quantity?.let {
                                if (it < item.quantity) {
                                    quantity = it + 1
                                }
                            }
                        },
                        onDecrement = {
                            quantity?.let {
                                if (it > 1) {
                                    quantity = it - 1
                                }
                            }
                        },
                        isDecrementEnabled = quantity != null && quantity!! in 2..item.quantity
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Price per Unit:")
                    IncrementalTextField(
                        value = price?.toString() ?: "",
                        onValueChange = { newValue ->
                            price = newValue.toIntOrNull()
                        },
                        onIncrement = {
                            price?.let {
                                price = it + 1
                            }
                        },
                        isIncrementEnabled = price != null,
                        onDecrement = {
                            price?.let {
                                price = it - 1
                            }
                        },
                        isDecrementEnabled = price != null && price!! > 0,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (quantity != null && price != null) {
                        onConfirm(quantity!!, price!!)
                    } else {
                        Toast.makeText(
                            context,
                            "Please enter a valid quantity and price",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) {
                Text(text = "Sell")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}