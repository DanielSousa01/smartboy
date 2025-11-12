package com.fcul.smartboy.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector


enum class Screens(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    MAP("map", "Map", Icons.Default.Place),
    PROFILE("profile", "Profile", Icons.Default.AccountBox),
    CHAT("chat", "Chat", Icons.Default.MailOutline),
    INVENTORY("inventory", "Inventory", Icons.Default.Star),
    CART("cart", "Cart", Icons.Default.ShoppingCart),
    WALLET("wallet", "Wallet", Icons.Default.AccountBox),
    SETTINGS("settings", "Settings", Icons.Default.Settings),
}

val bottomBarScreens = listOf(
    Screens.CHAT,
    Screens.INVENTORY,
    Screens.MAP,
    Screens.CART,
    Screens.WALLET,
)