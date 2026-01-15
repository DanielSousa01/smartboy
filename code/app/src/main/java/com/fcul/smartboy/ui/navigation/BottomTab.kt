package com.fcul.smartboy.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.fcul.smartboy.domain.navigation.Screen
import com.fcul.smartboy.domain.navigation.bottomBarScreens

@Composable
fun BottomTab(
    currentDestination: Screen,
    onDestinationChange: (Screen) -> Unit
) {
    NavigationBar {
        bottomBarScreens.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        screen.icon,
                        contentDescription = screen.label
                    )
                },
                label = { Text(screen.label) },
                selected = screen == currentDestination,
                onClick = { onDestinationChange(screen) }
            )
        }
    }
}