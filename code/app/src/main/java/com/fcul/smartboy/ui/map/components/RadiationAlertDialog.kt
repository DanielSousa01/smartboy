package com.fcul.smartboy.ui.map.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.route.RadiationData

@Composable
fun RadiationAlertDialog(
    radiationAlert: RadiationData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("⚠️", style = MaterialTheme.typography.displayMedium) },
        title = { Text("Radiation Zone Warning") },
        text = {
            Column {
                Text("You have entered a radiation zone!")
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                Text("Radiation Level: ${radiationAlert.radiationLevelInSv} Sv")
                Text("Affected Radius: ${radiationAlert.radius}m")
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                Text(
                    "⚠️ Take appropriate safety measures!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}