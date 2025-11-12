package com.fcul.smartboy.ui.navigation.drawer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DrawerTop() {
    Spacer(Modifier.height(12.dp))
    Text(
        "Drawer Title",
        modifier = Modifier.padding(16.dp),
        style = MaterialTheme.typography.titleLarge
    )
    HorizontalDivider()
}