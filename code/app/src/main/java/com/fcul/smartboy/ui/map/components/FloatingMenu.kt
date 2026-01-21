package com.fcul.smartboy.ui.map.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
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
import com.google.maps.android.compose.CameraPositionState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingMenu(
    pendingCheckpoints: List<LatLng>,
    cameraPositionState: CameraPositionState,
    checkpointSelected: LatLng?,
    isRouteActive: Boolean,
    isMenuOpen: Boolean,
    onAddPendingCheckpoint: () -> Unit,
    onClearSelectedCheckpoint: () -> Unit,
    onClearPendingCheckpoints: () -> Unit,
    onStartRoute: () -> Unit,
    onEndRoute: () -> Unit,
    onAddRadPoint: (LatLng) -> Unit,
    onMenuOpenChange: (Boolean) -> Unit
) {
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
            onClick = { onAddRadPoint(cameraPositionState.position.target) }
        )
        if (!isRouteActive) {
            FloatingActionButtonMenuItem(
                icon = { Icon(Icons.Default.PushPin, contentDescription = "Add Checkpoint") },
                text = { Text("Add Checkpoint") },
                onClick = {
                    onMenuOpenChange(false)
                    onAddPendingCheckpoint()
                }
            )
            if (pendingCheckpoints.isNotEmpty()) {
                FloatingActionButtonMenuItem(
                    icon = { Icon(Icons.Default.Clear, contentDescription = "Clear Checkpoints") },
                    text = { Text("Clear Checkpoints") },
                    onClick = {
                        onMenuOpenChange(false)
                        onClearPendingCheckpoints()
                    }
                )
            }
            if (pendingCheckpoints.size >= 2) {
                FloatingActionButtonMenuItem(
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Start Route") },
                    text = { Text("Start Route") },
                    onClick = {
                        onMenuOpenChange(false)
                        onStartRoute()
                    },
                )
            }
        } else {
            FloatingActionButtonMenuItem(
                icon = { Text("📍") },
                text = { Text("Add Checkpoint") },
                onClick = {
                    onMenuOpenChange(false)
                    onAddPendingCheckpoint()
                }
            )
            FloatingActionButtonMenuItem(
                icon = { Text("🏁") },
                text = { Text("End Route") },
                onClick = {
                    onMenuOpenChange(false)
                    onEndRoute()
                }
            )
        }
        if (checkpointSelected != null) {
            FloatingActionButtonMenuItem(
                icon = {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear Selected Checkpoint"
                    )
                },
                text = { Text("Clear Selected Checkpoint") },
                onClick = {
                    onMenuOpenChange(false)
                    onClearSelectedCheckpoint()
                }
            )
        }
    }
}