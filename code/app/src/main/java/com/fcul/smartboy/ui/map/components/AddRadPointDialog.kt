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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R
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
    var radiationLevelError by remember { mutableStateOf(false) }
    var radiusError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rad_add_radiation_point)) },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.location_format,
                        String.format(Locale.US, "%.6f", location.latitude),
                        String.format(Locale.US, "%.6f", location.longitude)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = radiationLevelText,
                    onValueChange = {
                        radiationLevelText = it
                        radiationLevelError = false
                    },
                    label = { Text(stringResource(R.string.rad_level_msv)) },
                    placeholder = { Text(stringResource(R.string.rad_level_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = radiationLevelError,
                    supportingText = if (radiationLevelError) {
                        { Text(stringResource(R.string.rad_error_valid_positive_number)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = radiusText,
                    onValueChange = {
                        radiusText = it
                        radiusError = false
                    },
                    label = { Text(stringResource(R.string.rad_radius_meters)) },
                    placeholder = { Text(stringResource(R.string.rad_radius_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = radiusError,
                    supportingText = if (radiusError) {
                        { Text(stringResource(R.string.rad_error_valid_positive_number)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val radiationLevel = radiationLevelText.toDoubleOrNull()
                    val radius = radiusText.toDoubleOrNull()

                    radiationLevelError = false
                    radiusError = false

                    if (radiationLevel == null || radiationLevel <= 0) {
                        radiationLevelError = true
                        return@TextButton
                    }

                    if (radius == null || radius <= 0) {
                        radiusError = true
                        return@TextButton
                    }

                    onConfirm(radiationLevel, radius)
                }
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}
