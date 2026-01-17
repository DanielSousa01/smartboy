package com.fcul.smartboy.ui.inventory.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.inventory.Item



@Composable
fun ItemDetails(
    item: Item,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    var quantity: Int? by remember { mutableStateOf(item.quantity) }

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
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                if (item is Item.Weapon && item.ammoId != null) {
                    Card {
                        Text(text = "Ammo : ${item.ammoName}")

                        Column(
                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
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
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card {
                            Text(text = "Quantity: ")
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        quantity?.let {
                                            quantity = if (it > 1) {
                                                it - 1
                                            } else {
                                                1
                                            }
                                        }
                                    },
                                    enabled = quantity != null && quantity!! > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease Quantity"
                                    )
                                }
                                TextField(
                                    value = quantity?.toString() ?: "",
                                    onValueChange = { newValue ->
                                        quantity = newValue.toIntOrNull()
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(80.dp)
                                        .padding(vertical = 4.dp),
                                )
                                IconButton(
                                    onClick = {
                                        quantity?.let {
                                            quantity = it + 1
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase Quantity"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onRemove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                    ) {
                    Text("Remove")
                }
                Button(
                    onClick = {
                    quantity?.let {
                        if (it > 0) {
                            onQuantityChange(it)
                            onDismiss()
                        } else {
                            Toast.makeText(
                                context,
                                "Quantity must be greater than 0",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                    enabled = quantity != null && item.quantity != quantity!! && quantity!! > 0
                ) {
                    Text("Update")
                }
                Button(onClick = onRemove) {
                    Text("Use")
                }
            }
        },
    )
}