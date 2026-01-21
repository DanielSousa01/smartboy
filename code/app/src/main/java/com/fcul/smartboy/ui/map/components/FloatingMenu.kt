package com.fcul.smartboy.ui.map.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingMenu(
    pendingCheckpoints: List<LatLng>,
    checkpointSelected: LatLng?,
    isRouteActive: Boolean,
    isMenuOpen: Boolean,
    onAddPendingCheckpoint: () -> Unit,
    onClearSelectedCheckpoint: () -> Unit,
    onClearPendingCheckpoints: () -> Unit,
    onStartRoute: () -> Unit,
    onEndRoute: () -> Unit,
    onAddRadPoint: () -> Unit,
    onMenuOpenChange: (Boolean) -> Unit
) {
    fun closeAnd(action: () -> Unit) {
        onMenuOpenChange(false)
        action()
    }

    val canStartRoute = pendingCheckpoints.size >= 2 && !isRouteActive
    val hasPending = pendingCheckpoints.isNotEmpty()
    val hasSelection = checkpointSelected != null

    FloatingActionButtonMenu(
        expanded = isMenuOpen,
        button = {
            FloatingActionButton(onClick = { onMenuOpenChange(!isMenuOpen) }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            icon = { Icon(Icons.Default.Add, contentDescription = "Add RAD") },
            text = { Text("Create RAD") },
            onClick = { closeAnd(onAddRadPoint) }
        )
        if (isRouteActive) {
            FloatingActionButtonMenuItem(
                icon = { Icon(Icons.Default.PushPin, contentDescription = "Add Checkpoint") },
                text = { Text("Add Checkpoint") },
                onClick = { closeAnd(onAddPendingCheckpoint) }
            )
            FloatingActionButtonMenuItem(
                icon = { Icon(Icons.Default.Flag, contentDescription = "End Route") },
                text = { Text("End Route") },
                onClick = { closeAnd(onEndRoute) }
            )
        } else {
            FloatingActionButtonMenuItem(
                icon = { Icon(Icons.Default.PushPin, contentDescription = "Add Checkpoint") },
                text = { Text("Add Checkpoint") },
                onClick = { closeAnd(onAddPendingCheckpoint) }
            )
            if (hasPending) {
                FloatingActionButtonMenuItem(
                    icon = { Icon(Icons.Default.Clear, contentDescription = "Clear Checkpoints") },
                    text = { Text("Clear Checkpoints") },
                    onClick = { closeAnd(onClearPendingCheckpoints) }
                )
            }
            if (canStartRoute) {
                FloatingActionButtonMenuItem(
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Start Route") },
                    text = { Text("Start Route") },
                    onClick = { closeAnd(onStartRoute) },
                )
            }
        }
        if (hasSelection) {
            FloatingActionButtonMenuItem(
                icon = {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear Selected Checkpoint"
                    )
                },
                text = { Text("Clear Selected Checkpoint") },
                onClick = { closeAnd(onClearSelectedCheckpoint) }
            )
        }
    }
}