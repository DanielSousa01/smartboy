package com.fcul.smartboy.ui.inventory.components.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.domain.inventory.SellingItem

@Composable
fun ItemCard(
    item: Item,
    sellingItem: SellingItem? = null,
    onQuantityChange: (Long, Int) -> Unit,
    onSellingItemQuantityChange: (Long, Int) -> Unit,
    onSellingItemValueChange: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onUse: (Long) -> Unit = {},
    onReload: (Long) -> Unit = {},
    onSell: (Long, Int, Int) -> Unit
) {
    var isItemDetailsOpen by remember { mutableStateOf(false) }

    if (isItemDetailsOpen) {
        ItemDetails(
            item = item,
            sellingItem = sellingItem,
            onUse = { onUse(item.id) },
            onReload = { onReload(item.id) },
            onDismiss = { isItemDetailsOpen = false },
            onRemove = {
                onRemove(item.id)
            },
            onQuantityChange = { newQuantity ->
                onQuantityChange(item.id, newQuantity)
            },
            onSell = { price, quantity ->
                onSell(item.id, price, quantity)
            },
            onSellingItemQuantityChange = { newQuantity ->
                if (sellingItem != null)
                    onSellingItemQuantityChange(sellingItem.id, newQuantity)
            },
            onSellingItemValueChange = { newValue ->
                if (sellingItem != null)
                    onSellingItemValueChange(sellingItem.id, newValue)
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
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    Text(
                        text = "${item.ammoLoaded}/${item.ammoMax}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ammo),
                        contentDescription = stringResource(R.string.cd_inventory_ammo_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = "${item.ammoName}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = "${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        thickness = 1.dp
                    )
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = stringResource(R.string.cd_inventory_description),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

        }
    }
}