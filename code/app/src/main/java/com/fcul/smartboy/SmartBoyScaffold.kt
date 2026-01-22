package com.fcul.smartboy

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fcul.smartboy.domain.navigation.Screen
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.ui.navigation.BottomTab
import com.fcul.smartboy.ui.navigation.DrawerNavigation
import com.fcul.smartboy.ui.navigation.NavGraph
import com.fcul.smartboy.ui.navigation.TopBar
import com.fcul.smartboy.ui.profile.drawer.ProfileDrawer
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun SmartBoyScaffold(
    navController: NavHostController,
    user: FirebaseUser?,
    userProfile: Profile?,
    onSignOut: () -> Unit
) {
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentRoute = navController
        .currentBackStackEntryAsState()
        .value
        ?.destination
        ?.route
    val currentScreen = Screen.fromRoute(currentRoute) ?: Screen.Map

    // Determine if bars should be shown based on route
    val showBars by remember(currentRoute) {
        derivedStateOf {
            currentRoute != Screen.Settings.route &&
                    !currentRoute.orEmpty().startsWith("chat_messages/")
        }
    }
    DrawerNavigation(
        rightDrawerState = rightDrawerState,
        leftDrawerState = leftDrawerState,
        leftDrawerContent = {
            ProfileDrawer(
                userName = user?.displayName ?: user?.email ?: stringResource(R.string.guest),
                userPicture = user?.photoUrl?.toString(),
                steps = userProfile?.steps ?: 0L,
                radiation = userProfile?.radiation ?: 0.0,
                radiationResistance = userProfile?.radiationResistance ?: 0.0,
                caps = userProfile?.caps ?: 0,
                onProfileClick = {
                    scope.launch { leftDrawerState.close() }
                    navController.navigate(Screen.Profile.route) {
                        launchSingleTop = true
                    }
                },
                onWalletClick = {
                    scope.launch { leftDrawerState.close() }
                    navController.navigate(Screen.Wallet.route) {
                        launchSingleTop = true
                    }
                },
                onSettingsClick = {
                    scope.launch { leftDrawerState.close() }
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onLogoutClick = {
                    scope.launch {
                        leftDrawerState.close()
                    }
                    onSignOut()
                }
            )
        },
        rightDrawerContent = {
        }
    ) {
        Scaffold(
            topBar = {
                if (showBars) {
                    TopBar(
                        onMenuClick = {
                            scope.launch {
                                rightDrawerState.close()
                                leftDrawerState.open()
                            }
                        },
                        onShoppingCartClick = {
                            scope.launch {
                                navController.navigate(Screen.Carts.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onScanPaymentClick = {
                            scope.launch {
                                navController.navigate(Screen.ScanPayment.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (showBars) {
                    BottomTab(
                        currentDestination = currentScreen,
                        onDestinationChange = { destination ->
                            navController.navigate(destination.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavGraph(
                user = user,
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}