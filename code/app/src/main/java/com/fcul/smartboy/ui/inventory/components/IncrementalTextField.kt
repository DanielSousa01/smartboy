package com.fcul.smartboy.ui.inventory.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R

@Composable
fun IncrementalTextField(
    modifier: Modifier = Modifier.padding(horizontal = 8.dp),
    value: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    isIncrementEnabled: Boolean = true,
    onDecrement: () -> Unit,
    isDecrementEnabled: Boolean = true,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = isDecrementEnabled,
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.cd_cart_decrease_value)
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier
                .width(80.dp)
                .padding(vertical = 4.dp),
        )
        IconButton(
            onClick = onIncrement,
            enabled = isIncrementEnabled,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_cart_increase_value)
            )
        }
    }
}