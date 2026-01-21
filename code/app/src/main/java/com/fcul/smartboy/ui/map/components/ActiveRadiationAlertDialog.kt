package com.fcul.smartboy.ui.map.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.fcul.smartboy.domain.route.RadiationData

@Composable
fun ActiveRadiationAlertDialog(
    alert: RadiationData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚠️ Radiation Alert") },
        text = {
            Text(
                "You entered a radiation zone!\n" +
                        "Level: ${alert.radiationLevelInSv} Sv\n" +
                        "Radius: ${alert.radius} m"
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
