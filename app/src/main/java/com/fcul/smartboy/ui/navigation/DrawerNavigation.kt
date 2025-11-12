package com.fcul.smartboy.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.ui.navigation.drawer.DrawerPartial

@Composable
fun DrawerNavigation(
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    leftDrawerContent: @Composable () -> Unit,
    rightDrawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            DrawerPartial()
                            rightDrawerContent()
                        }
                    }
                }
            },
            drawerState = rightDrawerState,
            gesturesEnabled = true
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ModalNavigationDrawer(
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                DrawerPartial()
                                leftDrawerContent()
                            }
                        }
                    },
                    drawerState = leftDrawerState,
                    gesturesEnabled = true
                ) {
                    content()
                }
            }
        }
    }
}