package com.fcul.smartboy.ui.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
        icon = {
            Icon(
                imageVector = Icons.Default.RadioButtonChecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.rad_add_radiation_point),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Location Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.location_format,
                                String.format(Locale.US, "%.6f", location.latitude),
                                String.format(Locale.US, "%.6f", location.longitude)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Radiation Level Input
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

                // Radius Input
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

                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "⚠️ This will create a radiation zone at this location",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val radiationLevel = radiationLevelText.toDoubleOrNull()
                    val radius = radiusText.toDoubleOrNull()

                    radiationLevelError = false
                    radiusError = false

                    if (radiationLevel == null || radiationLevel <= 0) {
                        radiationLevelError = true
                        return@Button
                    }

                    if (radius == null || radius <= 0) {
                        radiusError = true
                        return@Button
                    }

                    onConfirm(radiationLevel, radius)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}
