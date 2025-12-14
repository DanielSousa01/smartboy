package com.fcul.smartboy.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.google.firebase.auth.FirebaseUser

@ExperimentalMaterial3ExpressiveApi
@Composable
fun NavGraph(
    user: FirebaseUser?,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier
    ) {
        composable(Screen.Map.route) {
            val viewModel: MapViewmodel = hiltViewModel()
            val currentLocation by viewModel.currentLocation.collectAsState()
            val radSpots by viewModel.radSpots.collectAsState()
            val radiationAlert by viewModel.radiationAlert.collectAsState()
            val isRouteActive by viewModel.isRouteActive.collectAsState()
            val routeCheckpoints by viewModel.routeCheckpoints.collectAsState()
            val pendingCheckpoints by viewModel.pendingCheckpoints.collectAsState()
            val routePolyline by viewModel.routePolyline.collectAsState()

            MapScreen(
                currentLocation = currentLocation,
                radiationSpots = radSpots,
                radiationAlert = radiationAlert,
                isRouteActive = isRouteActive,
                routeCheckpoints = routeCheckpoints,
                pendingCheckpoints = pendingCheckpoints,
                routePolyline = routePolyline,
                onEnteringRadPoint = viewModel::onEnteringRadiationZone,
                onDismissAlert = viewModel::dismissRadiationAlert,
                onSetPoint = viewModel::setPoint,
                onCreateRadPoint = viewModel::createRadPoint,
                onLocationUpdate = viewModel::updateCurrentLocation,
                onStartRoute = viewModel::startRoute,
                onAddPendingCheckpoint = viewModel::addPendingCheckpoint,
                onAddCheckpoint = viewModel::addCheckpointToActiveRoute,
                onEndRoute = viewModel::endRoute,
            )
        }
        composable(Screen.Chat.route) { ChatScreen() }
        composable(Screen.Inventory.route) {
            val viewModel: InventoryViewmodel = hiltViewModel()
            InventoryScreen(
                itemsState = viewModel.items,
                isLoadingState = viewModel.isLoading,
                onReload = viewModel::reloadAmmo,
                onRemove = viewModel::removeItem,
                onQuantityChange = viewModel::changeQuantity,
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