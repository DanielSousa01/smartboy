package com.fcul.smartboy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fcul.smartboy.domain.navigation.Screen
import com.fcul.smartboy.ui.cart.CartScreen
import com.fcul.smartboy.ui.cart.CartsListScreen
import com.fcul.smartboy.ui.cart.ScanPaymentScreen
import com.fcul.smartboy.ui.cart.vm.CartViewModel
import com.fcul.smartboy.ui.chat.ChatMessagesScreen
import com.fcul.smartboy.ui.chat.ConversationsScreen
import com.fcul.smartboy.ui.chat.vm.ChatViewModel
import com.fcul.smartboy.ui.inventory.InventoryScreen
import com.fcul.smartboy.ui.inventory.vm.InventoryViewModel
import com.fcul.smartboy.ui.map.MapScreen
import com.fcul.smartboy.ui.map.vm.MapViewModel
import com.fcul.smartboy.ui.map.vm.NavigationEvent
import com.fcul.smartboy.ui.profile.ProfileScreen
import com.fcul.smartboy.ui.profile.vm.ProfileViewModel
import com.fcul.smartboy.ui.settings.SettingsScreen
import com.fcul.smartboy.ui.settings.vm.SettingsViewModel
import com.fcul.smartboy.ui.userdetails.UserDetailsScreen
import com.fcul.smartboy.ui.userdetails.vm.UserDetailsViewModel
import com.fcul.smartboy.ui.wallet.WalletScreen
import com.fcul.smartboy.ui.wallet.WalletViewModel
import com.fcul.smartboy.ui.common.QRCodeScanner
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
            val viewModel: MapViewModel = hiltViewModel(
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

            val lifecycleOwner = LocalLifecycleOwner.current

            // Reload location when screen becomes visible
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (currentLocation == null) {
                            viewModel.loadInitialLocation()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

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
            val viewModel: ChatViewModel = hiltViewModel()

            val conversations by viewModel.conversations.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()

            ConversationsScreen(
                conversations = conversations,
                isLoading = isLoading,
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
            val viewModel: ChatViewModel = hiltViewModel()

            val messages by viewModel.messages.collectAsState()
            val messageText by viewModel.messageText.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val error by viewModel.error.collectAsState()
            val userCaps by viewModel.userCaps.collectAsState()

            LaunchedEffect(userId) {
                viewModel.observeUserProfile(userId)
            }

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
                onBackClick = { navController.popBackStack() },
                onDismissError = viewModel::onDismissError
            )
        }
        composable(Screen.Inventory.route) {
            val viewModel: InventoryViewModel = hiltViewModel()
            val items by viewModel.items.collectAsState()
            val sellingItems by viewModel.sellingItems.collectAsState()
            val error by viewModel.error.collectAsState()

            InventoryScreen(
                items = items,
                sellingItems = sellingItems,
                userId = user?.uid,
                error = error,
                onUnload = viewModel::unloadAmmo,
                onReload = viewModel::reloadAmmo,
                onUseItem = viewModel::useItem,
                onRemoveItem = viewModel::removeItem,
                onItemQuantityChange = viewModel::changeItemQuantity,
                onSell = viewModel::sellItem,
                onRemoveSellingItem = viewModel::removeSellingItem,
                onSellingItemQuantityChange = viewModel::changeSellingItemQuantity,
                onSellingItemValueChange = viewModel::changeSellingItemValue,
                onDismissError = viewModel::onDismissError
            )
        }
        composable(Screen.Carts.route) {
            val viewModel: CartViewModel = hiltViewModel()

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
            val viewModel: CartViewModel = hiltViewModel()

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
                    onDismissError = viewModel::onDismissError,
                )
            }
        }
        composable(Screen.ScanPayment.route) {
            val viewModel: CartViewModel = hiltViewModel()

            ScanPaymentScreen(
                onCompletePurchase = viewModel::completePurchase,
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
                error = error,
                onDismissError = viewModel::onDismissError,
            )
        }
        composable(Screen.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()

            val profile by viewModel.profile.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val error by viewModel.error.collectAsState()

            ProfileScreen(
                profile = profile,
                user = user,
                error = error,
                isLoading = isLoading,
                onRefresh = viewModel::loadProfile,
                onDismissError = viewModel::onDismissError
            )
        }
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()

            val profile by viewModel.profile.collectAsState()
            val error by viewModel.error.collectAsState()

            SettingsScreen(
                profile = profile,
                error = error,
                onUpdateMeasurementUnit = viewModel::updateMeasurementUnit,
                onBackClick = { navController.popBackStack() },
                onDismissError = viewModel::onDismissError
            )
        }
    }
}