package com.fcul.smartboy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
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

@Composable
fun NavGraph(
    user: FirebaseUser?,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Persist ViewModels across navigation
    val activityViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "ViewModelStoreOwner is null"
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier
    ) {
        composable(Screen.Map.route) {
            // Scope ViewModel to activity level so it survives navigation
            val viewModel: MapViewmodel = hiltViewModel(
                viewModelStoreOwner = activityViewModelStoreOwner
            )
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
            val otherActiveRoutes by viewModel.otherActiveRoutes.collectAsState()

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
                otherActiveRoutes = otherActiveRoutes,
                onEnteringRadPoint = viewModel::onEnteringRadiationZone,
                onDismissAlert = viewModel::dismissRadiationAlert,
                onSetPoint = viewModel::setPoint,
                onCreateRadPoint = viewModel::createRadPoint,
                onRadiationMarkerClick = viewModel::onRadiationMarkerClick,
                onCheckpointMarkerClick = viewModel::onCheckpointMarkerClick,
                onClearMarkerSelection = viewModel::clearMarkerSelection,
                onStartRoute = viewModel::startRoute,
                onAddPendingCheckpoint = viewModel::addPendingCheckpoint,
                onEndRoute = viewModel::endRoute,
                onClearPendingCheckpoints = viewModel::clearPendingCheckpoints,
                onClearSelectedCheckpoint = viewModel::clearSelectedCheckpoint,
            )
        }
        composable(Screen.Chat.route) {
            val viewModel: ChatViewmodel = hiltViewModel()
            ConversationsScreen(
                viewModel = viewModel,
                onConversationClick = { conversation ->
                    val otherUserId = conversation.participantIds.firstOrNull {
                        it != user?.uid
                    } ?: return@ConversationsScreen

                    val otherUserName = conversation.participantNames[otherUserId] ?: "User"

                    navController.navigate(
                        Screen.ChatMessages.createRoute(otherUserId, otherUserName)
                    )
                }
            )
        }
        composable(Screen.ChatMessages.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val userName = backStackEntry.arguments?.getString("userName") ?: "User"
            val viewModel: ChatViewmodel = hiltViewModel()

            ChatMessagesScreen(
                viewModel = viewModel,
                userId = userId,
                userName = userName,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Inventory.route) {
            val viewModel: InventoryViewmodel = hiltViewModel()
            InventoryScreen(
                isLoadingState = viewModel.isLoading,
                itemsState = viewModel.items,
                sellingItemsState = viewModel.sellingItems,
                onUnload = viewModel::unloadAmmo,
                onReload = viewModel::reloadAmmo,
                onRemoveItem = viewModel::removeItem,
                onItemQuantityChange = viewModel::changeItemQuantity,
                onSell = viewModel::sellItem,
                onRemoveSellingItem = viewModel::removeSellingItem,
                onSellingItemQuantityChange = viewModel::changeSellingItemQuantity,
                onSellingItemValueChange = viewModel::changeSellingItemValue
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