package com.fcul.smartboy.ui.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.cart.Cart
import com.fcul.smartboy.ui.cart.components.CartCard
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CartsScreen(
    cartsState: StateFlow<List<Cart>>,
    onViewCart: (String?) -> Unit,
) {
    val carts by cartsState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Carts",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (carts.isNotEmpty()) {
                    items(carts) { cart ->
                        CartCard(
                            cart = cart,
                            onCartDetailsClick = {
                                onViewCart(cart.userId)
                            }
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "No carts available",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .weight(.1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onViewCart(null)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Add Cart")
            }

            Button(
                onClick = { /* Ação do segundo botão */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Confirm Sell")
            }
        }
    }

}