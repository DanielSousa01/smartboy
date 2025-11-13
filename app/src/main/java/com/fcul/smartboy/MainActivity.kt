package com.fcul.smartboy

import android.content.Intent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fcul.smartboy.ui.navigation.BottomTab
import com.fcul.smartboy.ui.navigation.DrawerNavigation
import com.fcul.smartboy.ui.navigation.NavGraph
import com.fcul.smartboy.ui.navigation.Screens
import com.fcul.smartboy.ui.navigation.TopBar
import com.fcul.smartboy.ui.theme.SmartBoyTheme
import com.fcul.smartboy.ui.navigation.drawer.left.LeftDrawer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        if (auth.currentUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val vm = SmartBoyViewModel(auth = auth)

        enableEdgeToEdge()
        setContent {
            SmartBoyTheme {
                SmartBoyApp(vm)
            }
        }
    }
}

class SmartBoyViewModel(
    auth: FirebaseAuth
) : ViewModel() {
    private var _user: MutableStateFlow<FirebaseUser?> = MutableStateFlow(null)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    init {
        val authUser = auth.currentUser ?: throw IllegalStateException("User must be logged in")
        _user.value = authUser
    }

    fun onProfileClick() {
        // Handle profile click
    }

    fun onSettingsClick() {
        // Handle settings click
    }

    fun onLogoutClick() {
        // Handle logout click
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartBoyApp(
    vm: SmartBoyViewModel,
) {
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val user by vm.user.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = Screens.entries.firstOrNull { it.route == currentRoute } ?: Screens.MAP

    DrawerNavigation (
        rightDrawerState = rightDrawerState,
        leftDrawerState = leftDrawerState,

        leftDrawerContent = { LeftDrawer(
            userName = user?.displayName ?: user?.email ?: "Guest",
            userPicture = user?.photoUrl?.toString(),
            onProfileClick = { vm.onProfileClick() },
            onSettingsClick = { vm.onSettingsClick() },
            onLogoutClick = { vm.onLogoutClick() }
        ) },
        rightDrawerContent = {}
    ) {
        BottomTab(
            currentDestination = currentScreen,
            onDestinationChange = { destination ->
                navController.navigate(destination.route) {
                    // Avoid building up backstack for repeated taps
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
        {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    onMenuClick = { scope.launch { leftDrawerState.open() } },
                    onDestinationChange = {
                        navController.navigate(Screens.WALLET.route) {
                            // Avoid building up backstack for repeated taps
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onShoppingCartClick = { scope.launch { rightDrawerState.open() } },
                )
                NavGraph(navController = navController)
            }
        }
    }
}