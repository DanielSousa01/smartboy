package com.fcul.smartboy.ui.map.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

@Composable
fun AddRadPointDialog(
    location: LatLng,
    onDismiss: () -> Unit,
    onConfirm: (radiationLevel: Double, radius: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var radiationLevelText by remember { mutableStateOf("") }
    var radiusText by remember { mutableStateOf("") }
    var radiationLevelError by remember { mutableStateOf<String?>(null) }
    var radiusError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Radiation Point") },
        text = {
            Column {
                Text(
                    text = "Location: ${
                        String.format(
                            Locale.US,
                            "%.6f",
                            location.latitude
                        )
                    }, ${String.format(Locale.US, "%.6f", location.longitude)}",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = radiationLevelText,
                    onValueChange = {
                        radiationLevelText = it
                        radiationLevelError = null
                    },
                    label = { Text("Radiation Level (Sv)") },
                    placeholder = { Text("e.g., 0.05") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = radiationLevelError != null,
                    supportingText = radiationLevelError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = radiusText,
                    onValueChange = {
                        radiusText = it
                        radiusError = null
                    },
                    label = { Text("Radius (meters)") },
                    placeholder = { Text("e.g., 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = radiusError != null,
                    supportingText = radiusError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate inputs
                    val radiationLevel = radiationLevelText.toDoubleOrNull()
                    val radius = radiusText.toDoubleOrNull()

                    var hasError = false

                    if (radiationLevel == null || radiationLevel <= 0) {
                        radiationLevelError = "Please enter a valid positive number"
                        hasError = true
                    }

                    if (radius == null || radius <= 0) {
                        radiusError = "Please enter a valid positive number"
                        hasError = true
                    }

                    if (!hasError && radiationLevel != null && radius != null) {
                        onConfirm(radiationLevel, radius)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

