package com.fcul.smartboy.domain.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.fcul.smartboy.R


sealed class Screen(
    val route: String,
    val labelResId: Int,
    val icon: ImageVector
) {
    object Map : Screen("map", R.string.map, Icons.Default.Place)
    object Profile : Screen("profile", R.string.profile, Icons.Default.AccountBox)
    object Chat : Screen("chat", R.string.chat, Icons.Default.MailOutline)
    object ChatMessages : Screen("chat_messages/{userId}/{userName}", R.string.chat, Icons.Default.MailOutline) {
        fun createRoute(userId: String, userName: String) = "chat_messages/$userId/$userName"
    }
    object Inventory : Screen("inventory", R.string.inventory, Icons.Default.Star)
    object Cart : Screen("cart", R.string.cart, Icons.Default.ShoppingCart)
    object Wallet : Screen("wallet", R.string.wallet, Icons.Default.AccountBox)
    object Settings : Screen("settings", R.string.settings, Icons.Default.Settings)

    companion object {
        val allScreens = listOf(
            Map,
            Profile,
            Chat,
            ChatMessages,
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