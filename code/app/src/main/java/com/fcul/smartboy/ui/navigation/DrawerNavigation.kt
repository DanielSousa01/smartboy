package com.fcul.smartboy.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DrawerNavigation(
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    leftDrawerContent: @Composable () -> Unit,
    rightDrawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    if (leftDrawerState.isOpen) {
        BackHandler {
            scope.launch { leftDrawerState.close() }
        }
    }

    if (rightDrawerState.isOpen) {
        BackHandler {
            scope.launch { rightDrawerState.close() }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            rightDrawerContent()
                        }
                    }
                }
            },
            drawerState = rightDrawerState,
            gesturesEnabled = rightDrawerState.isOpen
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ModalNavigationDrawer(
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                leftDrawerContent()
                            }
                        }
                    },
                    drawerState = leftDrawerState,
                    gesturesEnabled = leftDrawerState.isOpen
                ) {
                    content()
                }
            }
        }
    }
}