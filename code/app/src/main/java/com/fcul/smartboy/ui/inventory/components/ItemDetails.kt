package com.fcul.smartboy.ui.inventory.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.inventory.Item

//data class ItemEntity(
//    val id: Long = 0,
//    val name: String = "",
//    val quantity: Int = 0,
//    val category: String = "",
//    val ammoId: Long? = null,
//    val ammoName: String? = null,
//    val ammoMax: Int? = null,
//    val ammoLoaded: Int? = null
//) {
//
//}
@Composable
fun ItemDetails(
    item: Item,
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
                Text(text = item.name, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Quantity: ")
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                quantity?.let {
                                    if (it > 1) {
                                        quantity = it - 1
                                    } else {
                                        quantity = 1
                                    }
                                }
                            }
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
                Text(text = "Category: ${item.category}")
                if (item is Item.Weapon) {
                    if (item.ammoId != null) {
                        Text(text = "Ammo : ${item.ammoName}")
                        Text(text = "Ammo Max : ${item.ammoMax}")
                        Text(text = "Ammo Loaded : ${item.ammoLoaded}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                quantity?.let {
                    if (it > 0) {
                        onQuantityChange(it)
                        onDismiss()
                    } else {
//                      Check if the context should come from the screen
                        Toast.makeText(
                            context,
                            "Quantity must be greater than 0",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            ) {
                Text("Update Quantity")
            }
        },
        dismissButton = {
            TextButton(onClick = onRemove) {
                Text("Remove")
            }
        },
    )
}