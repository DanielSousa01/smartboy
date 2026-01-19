package com.fcul.smartboy

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fcul.smartboy.domain.navigation.Screen
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.ui.navigation.BottomTab
import com.fcul.smartboy.ui.navigation.DrawerNavigation
import com.fcul.smartboy.ui.navigation.NavGraph
import com.fcul.smartboy.ui.navigation.TopBar
import com.fcul.smartboy.ui.navigation.drawer.Drawer
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@ExperimentalMaterial3ExpressiveApi
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


    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route
    val currentScreen = Screen.fromRoute(currentRoute) ?: Screen.Map

    // Determine if bars should be shown based on route
    val showBars = currentRoute != Screen.Settings.route

    DrawerNavigation(
        rightDrawerState = rightDrawerState,
        leftDrawerState = leftDrawerState,
        leftDrawerContent = {
            Drawer(
                userName = user?.displayName ?: user?.email ?: "Guest",
                userPicture = user?.photoUrl?.toString(),
                steps = userProfile?.steps ?: 0L,
                radiation = userProfile?.radiation ?: 0.0,
                caps = userProfile?.caps ?: 0,
                onProfileClick = {
                    scope.launch { leftDrawerState.close() }
                    navController.navigate(Screen.Profile.route) {
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
                    scope.launch { onSignOut() }
                }
            )
        },
        rightDrawerContent = {}
    ) {
        Scaffold(
            topBar = {
                if (showBars) {
                    TopBar(
                        onMenuClick = { scope.launch { leftDrawerState.open() } },
                        onDestinationChange = {
                            navController.navigate(Screen.Wallet.route) {
                                launchSingleTop = true
                            }
                        },
                        onShoppingCartClick = { scope.launch { rightDrawerState.open() } }
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