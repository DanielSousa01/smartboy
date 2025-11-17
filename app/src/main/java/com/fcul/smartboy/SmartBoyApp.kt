package com.fcul.smartboy

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmartBoyApp(vm: MainViewmodel) {
    val user by vm.user.collectAsState()
    val navController = rememberNavController()

    SmartBoyScaffold(
        navController = navController,
        user = user
    )
}
