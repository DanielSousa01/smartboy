package com.fcul.smartboy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fcul.smartboy.domain.navigation.Screen
import com.fcul.smartboy.ui.cart.CartScreen
import com.fcul.smartboy.ui.cart.CartsListScreen
import com.fcul.smartboy.ui.cart.CartViewmodel
import com.fcul.smartboy.ui.cart.ScanPaymentScreen
import com.fcul.smartboy.ui.chat.ChatMessagesScreen
import com.fcul.smartboy.ui.chat.ChatViewmodel
import com.fcul.smartboy.ui.chat.ConversationsScreen
import com.fcul.smartboy.ui.inventory.InventoryScreen
import com.fcul.smartboy.ui.inventory.InventoryViewmodel
import com.fcul.smartboy.ui.map.MapScreen
import com.fcul.smartboy.ui.map.MapViewmodel
import com.fcul.smartboy.ui.map.NavigationEvent
import com.fcul.smartboy.ui.profile.ProfileScreen
import com.fcul.smartboy.ui.profile.ProfileViewmodel
import com.fcul.smartboy.ui.settings.SettingsScreen
import com.fcul.smartboy.ui.userdetails.UserDetailsScreen
import com.fcul.smartboy.ui.userdetails.UserDetailsViewModel
import com.fcul.smartboy.ui.wallet.WalletScreen
import com.fcul.smartboy.ui.wallet.WalletViewModel
import com.fcul.smartboy.utils.QRCodeScanner
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
            val navigationEvent by viewModel.navigationEvent.collectAsState()

            // Handle navigation events
            LaunchedEffect(navigationEvent) {
                when (val event = navigationEvent) {
                    is NavigationEvent.NavigateToUserDetails -> {
                        navController.navigate(Screen.UserDetails.createRoute(event.userId))
                        viewModel.clearNavigationEvent()
                    }
                    null -> { /* No event */ }
                }
            }

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
                onUserMarkerClick = viewModel::onUserClick
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

            val messages by viewModel.messages.collectAsState()
            val messageText by viewModel.messageText.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val error by viewModel.error.collectAsState()
            val userCaps by viewModel.userCaps.collectAsState()

            ChatMessagesScreen(
                messages = messages,
                messageText = messageText,
                userId = userId,
                userName = userName,
                isLoading = isLoading,
                error = error,
                userCaps = userCaps,
                onSendImage = viewModel::sendImageMessage,
                onStartChat = viewModel::startChat,
                onClearError = viewModel::clearError,
                onSendMessage = viewModel::sendMessage,
                onUpdateMessageText = viewModel::updateMessageText,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.UserDetails.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val viewModel: UserDetailsViewModel = hiltViewModel()

            val profile by viewModel.profile.collectAsState()
            val sellingItems by viewModel.sellingItems.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val error by viewModel.error.collectAsState()

            LaunchedEffect(userId) {
                viewModel.loadUserProfile(userId)
            }

            UserDetailsScreen(
                profile = profile,
                sellingItems = sellingItems,
                isLoading = isLoading,
                error = error,
                onSendMessage = { targetUserId ->
                    val userName = profile?.username ?: "User"
                    navController.navigate(
                        Screen.ChatMessages.createRoute(targetUserId, userName)
                    )
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Inventory.route) {
            val viewModel: InventoryViewmodel = hiltViewModel()
            val items by viewModel.items.collectAsState()
            val sellingItems by viewModel.sellingItems.collectAsState()

            InventoryScreen(
                items = items,
                sellingItems = sellingItems,
                userId = user?.uid,
                onUnload = viewModel::unloadAmmo,
                onReload = viewModel::reloadAmmo,
                onUseItem = viewModel::useItem,
                onRemoveItem = viewModel::removeItem,
                onItemQuantityChange = viewModel::changeItemQuantity,
                onSell = viewModel::sellItem,
                onRemoveSellingItem = viewModel::removeSellingItem,
                onSellingItemQuantityChange = viewModel::changeSellingItemQuantity,
                onSellingItemValueChange = viewModel::changeSellingItemValue
            )
        }
        composable(Screen.Carts.route) {
            val viewModel: CartViewmodel = hiltViewModel()

            val carts by viewModel.carts.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()

            var showScanner by remember { mutableStateOf(false) }

            if (showScanner) {
                QRCodeScanner(
                    onQRCodeScanned = { qrData ->
                        try {
                            val parts = qrData.split("/")
                            if (parts.size == 2) {
                                val sellerId = parts[0]
                                val itemId = parts[1].toLongOrNull()

                                if (itemId != null) {
                                    // Add item to cart (creates cart if doesn't exist)
                                    viewModel.getSellingItem(sellerId, itemId)
                                    // Navigate to the seller's cart
                                    navController.navigate(Screen.Cart.createRoute(sellerId))
                                }
                            }
                        } catch (e: Exception) {
                            // Invalid QR format
                        }
                        showScanner = false
                    },
                    onClose = {
                        showScanner = false
                    }
                )
            } else {
                CartsListScreen(
                    carts = carts,
                    isLoading = isLoading,
                    onCartSelected = { sellerId ->
                        navController.navigate(Screen.Cart.createRoute(sellerId))
                    },
                    onScanProduct = {
                        showScanner = true
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.Cart.route) { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: return@composable
            val viewModel: CartViewmodel = hiltViewModel()

            // Select this cart
            LaunchedEffect(sellerId) {
                viewModel.selectCart(sellerId)
            }

            val carts by viewModel.carts.collectAsState()
            val selectedCart = carts[sellerId]
            val isLoading by viewModel.isLoading.collectAsState()
            val qrCodeBitmap by viewModel.qrCodeBitmap.collectAsState()
            val error by viewModel.error.collectAsState()

            var showScanner by remember { mutableStateOf(false) }

            if (showScanner) {
                QRCodeScanner(
                    onQRCodeScanned = { qrData ->
                        // Parse QR code format: sellerId/itemId
                        try {
                            val parts = qrData.split("/")
                            if (parts.size == 2) {
                                val scannedSellerId = parts[0]
                                val itemId = parts[1].toLongOrNull()

                                if (itemId != null) {
                                    viewModel.getSellingItem(scannedSellerId, itemId)
                                }
                            }
                        } catch (e: Exception) {
                            // Invalid QR format
                        }
                        showScanner = false
                    },
                    onClose = {
                        showScanner = false
                    }
                )
            } else {
                CartScreen(
                    currentCart = selectedCart,
                    isLoading = isLoading,
                    qrCodeBitmap = qrCodeBitmap,
                    error = error,
                    onRemoveItem = viewModel::removeItemFromCart,
                    onUpdateQuantity = viewModel::updateItemQuantity,
                    onClearCart = viewModel::clearCart,
                    onGenerateQRCode = viewModel::generatePaymentQRCode,
                    onScanQRCode = {
                        showScanner = true
                    },
                    onDismissQRCode = viewModel::dismissQRCode,
                    onDismissError = viewModel::dismissError
                )
            }
        }
        composable(Screen.ScanPayment.route) {
            val viewModel: CartViewmodel = hiltViewModel()

            ScanPaymentScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onPaymentComplete = { navController.popBackStack() }
            )
        }
        composable(Screen.Wallet.route) {
            val viewModel: WalletViewModel = hiltViewModel()

            val transactions by viewModel.transactions.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val error by viewModel.error.collectAsState()

            WalletScreen(
                transactions = transactions,
                isLoading = isLoading,
                error = error
            )
        }
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