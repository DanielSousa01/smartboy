package com.fcul.smartboy.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onMenuClick: () -> Unit,
    onDestinationChange: () -> Unit,
    onShoppingCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    IconButton(
                        onClick = { onMenuClick() }
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                }
                Column(
                    modifier = modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SmartBoy")
                }
                Column(
                    modifier = modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
//                  TODO: improve this part
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("10000")
                        IconButton(
                            onClick = { onDestinationChange() }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.capicon),
                                contentDescription = "Menu",
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                        }
                        IconButton(
                            onClick = { onShoppingCartClick() }
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Menu"
                            )
                        }
                    }
                }
            }
        }
    )
}