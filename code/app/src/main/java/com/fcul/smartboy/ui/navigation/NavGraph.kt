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
import com.fcul.smartboy.ui.chat.ChatMessagesScreen
import com.fcul.smartboy.ui.chat.ChatViewmodel
import com.fcul.smartboy.ui.chat.ConversationsScreen
import com.fcul.smartboy.ui.inventory.InventoryScreen
import com.fcul.smartboy.ui.inventory.InventoryViewmodel
import com.fcul.smartboy.ui.map.MapScreen
import com.fcul.smartboy.ui.map.MapViewmodel
import com.fcul.smartboy.ui.profile.ProfileScreen
import com.fcul.smartboy.ui.profile.ProfileViewmodel
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
        composable(Screen.Map.route) { backStackEntry ->
            // Scope ViewModel to the backStackEntry to prevent recreation
            val viewModel: MapViewmodel = hiltViewModel(backStackEntry)
            val currentLocation by viewModel.currentLocation.collectAsState()
            val radSpots by viewModel.radSpots.collectAsState()
            val radiationAlert by viewModel.radiationAlert.collectAsState()
            val isRouteActive by viewModel.isRouteActive.collectAsState()
            val pendingCheckpoints by viewModel.pendingCheckpoints.collectAsState()
            val routePolyline by viewModel.routePolyline.collectAsState()
            val routeInfo by viewModel.routeInfo.collectAsState()
            val traveledPath by viewModel.traveledPath.collectAsState()
            val remainingRoute by viewModel.remainingRoute.collectAsState()
            val selectedRadiationMarker by viewModel.selectedRadiationMarker.collectAsState()
            val selectedCheckpointMarker by viewModel.selectedCheckpointMarker.collectAsState()

            MapScreen(
                currentLocation = currentLocation,
                radiationSpots = radSpots,
                radiationAlert = radiationAlert,
                isRouteActive = isRouteActive,
                pendingCheckpoints = pendingCheckpoints,
                routePolyline = routePolyline,
                routeInfo = routeInfo,
                traveledPath = traveledPath,
                remainingRoute = remainingRoute,
                selectedRadiationMarker = selectedRadiationMarker,
                selectedCheckpointMarker = selectedCheckpointMarker,
                onEnteringRadPoint = viewModel::onEnteringRadiationZone,
                onDismissAlert = viewModel::dismissRadiationAlert,
                onSetPoint = viewModel::setPoint,
                onCreateRadPoint = viewModel::createRadPoint,
                onLocationUpdate = viewModel::updateCurrentLocation,
                onRadiationMarkerClick = viewModel::onRadiationMarkerClick,
                onCheckpointMarkerClick = viewModel::onCheckpointMarkerClick,
                onClearMarkerSelection = viewModel::clearMarkerSelection,
                onStartRoute = viewModel::startRoute,
                onAddPendingCheckpoint = viewModel::addPendingCheckpoint,
                onEndRoute = viewModel::endRoute,
                onClearPendingCheckpoints = viewModel::clearPendingCheckpoints,
                onClearSelectedCheckpoint = viewModel::clearSelectedCheckpoint
            )
        }
        composable(Screen.Chat.route) {
            val viewModel: ChatViewmodel = hiltViewModel()
            val viewState by viewModel.viewState.collectAsState()

            when (viewState) {
                "conversations" -> ConversationsScreen(viewModel)
                "chat" -> ChatMessagesScreen(viewModel)
            }
        }
        composable(Screen.Inventory.route) {
            val viewModel: InventoryViewmodel = hiltViewModel()
            InventoryScreen(
                itemsState = viewModel.items,
                sellingItemsState = viewModel.sellingItems,
                isLoadingState = viewModel.isLoading,
                onUnload = viewModel::unloadAmmo,
                onReload = viewModel::reloadAmmo,
                onRemove = viewModel::removeItem,
                onQuantityChange = viewModel::changeQuantity,
                onSell = viewModel::sellItem,
            )
        }
        composable(Screen.Cart.route) { CartScreen() }
        composable(Screen.Wallet.route) { WalletScreen() }
        composable(Screen.Profile.route) {
            val viewmodel: ProfileViewmodel = hiltViewModel()

            val profile by viewmodel.profile.collectAsState()
            val isLoading by viewmodel.isLoading.collectAsState()
            val error by viewmodel.error.collectAsState()

            ProfileScreen(
                profile = profile,
                user = user,
                error = error,
                isLoading = isLoading,
                onRefresh = viewmodel::refresh
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}