package com.fcul.smartboy.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fcul.smartboy.domain.navigation.Screen
import com.fcul.smartboy.ui.cart.CartScreen
import com.fcul.smartboy.ui.chat.ChatScreen
import com.fcul.smartboy.ui.inventory.InventoryScreen
import com.fcul.smartboy.ui.inventory.InventoryViewmodel
import com.fcul.smartboy.ui.map.MapScreen
import com.fcul.smartboy.ui.map.MapViewmodel
import com.fcul.smartboy.ui.profile.ProfileScreen
import com.fcul.smartboy.ui.settings.SettingsScreen
import com.fcul.smartboy.ui.wallet.WalletScreen

@ExperimentalMaterial3ExpressiveApi
@Composable
fun NavGraph(
    navController: NavHostController,
    mapViewmodel: MapViewmodel,
    inventoryViewmodel: InventoryViewmodel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier
    ) {
        composable(Screen.Map.route) {
            MapScreen(
                onSetPoint = mapViewmodel::setPoint,
                onClearPoint = mapViewmodel::clearPoint
            )
        }
        composable(Screen.Chat.route) { ChatScreen() }
        composable(Screen.Inventory.route) {
            InventoryScreen(
                itemsState = inventoryViewmodel.items,
                onReload = inventoryViewmodel::reloadAmmo,
            )
        }
        composable(Screen.Cart.route) { CartScreen() }
        composable(Screen.Wallet.route) { WalletScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
        composable(Screen.Settings.route) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}