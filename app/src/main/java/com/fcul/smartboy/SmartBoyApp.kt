package com.fcul.smartboy

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.fcul.smartboy.ui.inventory.InventoryViewmodel
import com.fcul.smartboy.ui.map.MapViewmodel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmartBoyApp(vm: MainViewmodel) {
    val user by vm.user.collectAsState()
    val navController = rememberNavController()

    // ViewModels
    val mapViewmodel = MapViewmodel()
    val inventoryViewmodel = InventoryViewmodel()

    SmartBoyScaffold(
        navController = navController,
        mapViewmodel = mapViewmodel,
        inventoryViewmodel = inventoryViewmodel,
        user = user
    )
}
