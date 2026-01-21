package com.fcul.smartboy.ui.inventory.components.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.fcul.smartboy.domain.inventory.Item

@Composable
fun ItemCard(
    item: Item,
    onQuantityChange: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onUse: (Long) -> Unit = {},
    onReload: (Long) -> Unit = {},
    onSell: (Long, Int, Int) -> Unit
) {
    var isItemDetailsOpen by remember { mutableStateOf(false) }

    if (isItemDetailsOpen) {
        ItemDetails(
            item = item,
            onUse = { onUse(item.id) },
            onReload = { onReload(item.id) },
            onDismiss = { isItemDetailsOpen = false },
            onRemove = {
                onRemove(item.id)
                isItemDetailsOpen = false
            },
            onQuantityChange = { newQuantity ->
                onQuantityChange(item.id, newQuantity)
                isItemDetailsOpen = false
            },
            onSell = { price, quantity ->
                onSell(item.id, price, quantity)
                isItemDetailsOpen = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = { isItemDetailsOpen = true }),
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
                Row {
                    if (item.ammoName != null) {
                        Text(
                            text = "Ammo Name : ${item.ammoName}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
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