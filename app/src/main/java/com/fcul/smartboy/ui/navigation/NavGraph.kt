package com.fcul.smartboy.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fcul.smartboy.ui.cart.CartScreen
import com.fcul.smartboy.ui.chat.ChatScreen
import com.fcul.smartboy.ui.home.HomeScreen
import com.fcul.smartboy.ui.inventory.InventoryScreen
import com.fcul.smartboy.ui.profile.ProfileScreen
import com.fcul.smartboy.ui.settings.SettingsScreen
import com.fcul.smartboy.ui.wallet.WalletScreen

@Composable
fun NavGraph(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = Screens.MAP.route,
    ) {
        composable(route = Screens.MAP.route) {
            HomeScreen()
        }
        composable(route = Screens.PROFILE.route) {
            ProfileScreen()
        }
        composable(route = Screens.CHAT.route) {
            ChatScreen()
        }
        composable(route = Screens.INVENTORY.route) {
            InventoryScreen()
        }
        composable(route = Screens.CART.route) {
            CartScreen()
        }
        composable(route = Screens.WALLET.route) {
            WalletScreen()
        }
        composable(route = Screens.SETTINGS.route) {
            SettingsScreen()
        }
    }
}