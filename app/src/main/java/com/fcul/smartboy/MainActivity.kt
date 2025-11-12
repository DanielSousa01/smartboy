package com.fcul.smartboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.fcul.smartboy.ui.navigation.BottomTab
import com.fcul.smartboy.ui.navigation.DrawerNavigation
import com.fcul.smartboy.ui.navigation.NavGraph
import com.fcul.smartboy.ui.navigation.Screens
import com.fcul.smartboy.ui.navigation.TopBar
import com.fcul.smartboy.ui.theme.SmartBoyTheme
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartBoyTheme {
                SmartBoyApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartBoyApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = Screens.entries.firstOrNull { it.route == currentRoute } ?: Screens.MAP

    DrawerNavigation (
        drawerState = drawerState
    ){
        BottomTab(
            currentDestination = currentScreen,
            onDestinationChange = { destination ->
                navController.navigate(destination.route) {
                    // Avoid building up backstack for repeated taps
                    launchSingleTop = true
                    restoreState = true
                }
            },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(onMenuClick = { scope.launch { drawerState.open() } })
                NavGraph(navController = navController, modifier = Modifier.weight(1f))
            }
        }
    }
}