package com.fcul.smartboy.domain.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector


sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Map : Screen("map", "Map", Icons.Default.Place)
    object Profile : Screen("profile", "Profile", Icons.Default.AccountBox)
    object Chat : Screen("chat", "Chat", Icons.Default.MailOutline)
    object Inventory : Screen("inventory", "Inventory", Icons.Default.Star)
    object Cart : Screen("cart", "Cart", Icons.Default.ShoppingCart)
    object Wallet : Screen("wallet", "Wallet", Icons.Default.AccountBox)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val allScreens = listOf(
            Map,
            Profile,
            Chat,
            Inventory,
            Cart,
            Wallet,
            Settings
        )

        fun fromRoute(route: String?): Screen? =
            allScreens.firstOrNull { it.route == route }
    }
}

val bottomBarScreens = listOf(
    Screen.Chat,
    Screen.Map,
    Screen.Inventory
)