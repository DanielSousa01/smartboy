package com.fcul.smartboy.ui.map.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.route.RadiationData

@Composable
fun RadiationAlertDialog(
    radiationAlert: RadiationData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.cd_rad_warning),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.rad_zone_warning)) },
        text = {
            Column {
                Text(stringResource(R.string.rad_alert_message_1))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.rad_alert_message_2) + " " + radiationAlert.radiationLevelInMSv + " MSv")
                Text(stringResource(R.string.rad_alert_message_3) + " " + radiationAlert.radius + " m")
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Take appropriate safety measures!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
            }
        }
    )
}
