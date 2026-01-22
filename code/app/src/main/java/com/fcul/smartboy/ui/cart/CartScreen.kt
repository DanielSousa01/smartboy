package com.fcul.smartboy.ui.cart

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fcul.smartboy.domain.cart.Cart
import com.fcul.smartboy.domain.cart.ProductId
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.ui.cart.components.CartItemCard
import com.fcul.smartboy.ui.cart.components.CartItemDetails
import com.fcul.smartboy.utils.QRCodeScanner

@Composable
fun CartScreen(
    currentCart: Cart?,
    isLoading: Boolean,
    qrCodeBitmap: Bitmap?,
    error: String?,
    onRemoveItem: (Long) -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onClearCart: () -> Unit,
    onGenerateQRCode: () -> Unit,
    onScanQRCode: () -> Unit,
    onDismissQRCode: () -> Unit,
    onDismissError: () -> Unit,
//    getSellingItem: (String, Long) -> SellingItem?
) {
    var readQR by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<SellingItem?>(null) }

//    if (readQR) {
//        QRCodeScanner(
//            onQRCodeScanned = { value ->
//                Log.d("QRCodeScanner", "Scanned QR Code: $value")
//                val productId = parseSellingId(value)
//                val item = getSellingItem(productId.userId, productId.productId)
//                readQR = false
//
//                if (item != null) {
//                    selectedItem = item
//                }
//            },
//            onClose = {
//                readQR = false
//            }
//        )
//    } else {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 16.dp)
//                .weight(1f),
//            horizontalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
//            ) {
//                if (cart.items.isNotEmpty()) {
//                    items(cart.items) { item ->
//                        CartItemCard(
//                            item = item,
//                            onRemove = {
//                                // Handle item removal
//                            }
//                        )
//                    }
//                } else {
//                    item {
//                        Text(
//                            text = "No carts available",
//                            style = MaterialTheme.typography.bodyMedium,
//                            modifier = Modifier.padding(vertical = 4.dp)
//                        )
//                    }
//                }
//            }
//        }


    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                currentCart != null -> {
                    CartContent(
                        cart = currentCart,
                        onRemoveItem = onRemoveItem,
                        onUpdateQuantity = onUpdateQuantity,
                        onClearCart = onClearCart,
                        onGenerateQRCode = onGenerateQRCode,
                        onScanQRCode = onScanQRCode
                    )
                }

                else -> {
                    Text(
                        text = "Failed to load cart",
                        modifier = Modifier.align(Alignment.Center)
                    )


                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = cart.userName?.let { "Cart for $it Items" } ?: "New Cart",
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (selectedItem != null) {
                            CartItemDetails(
                                item = selectedItem!!,
                                buyingView = true,
                                onDismiss = { selectedItem = null },
                                onAddToCart = {},
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    readQR = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add Item")
                            }
                        }
                    }
                }

                // QR Code Dialog
                qrCodeBitmap?.let { bitmap ->
                    QRCodeDialog(
                        bitmap = bitmap,
                        totalPrice = currentCart?.totalPrice ?: 0,
                        onDismiss = onDismissQRCode
                    )
                }

                        // Error Snackbar
                        error ?. let { errorMessage ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = onDismissError) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(errorMessage)
                    }
                }
            }
        }
    }
}

private fun parseSellingId(id: String): ProductId {
    val values = id.split("/")
    return ProductId(values[0], values[1].toLong())
}

@Composable
private fun CartContent(
    cart: Cart,
    onRemoveItem: (Long) -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onClearCart: () -> Unit,
    onGenerateQRCode: () -> Unit,
    onScanQRCode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shopping Cart",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (cart.items.isNotEmpty()) {
                IconButton(onClick = onClearCart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear cart"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scan QR Code Button (always visible)
        OutlinedButton(
            onClick = onScanQRCode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Product QR Code")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cart items
        if (cart.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Your cart is empty",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Browse the marketplace to add items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cart.items, key = { it.id }) { item ->
                    CartItemCard(
                        item = item,
                        onRemove = { onRemoveItem(item.id) },
                        onQuantityChange = { newQuantity ->
                            onUpdateQuantity(item.id, newQuantity)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total and checkout
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total:",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${cart.totalPrice} Caps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${cart.items.size} item(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onGenerateQRCode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Payment QR Code")
                    }
                }
            }
        }
    }
}

@Composable
private fun QRCodeDialog(
    bitmap: Bitmap,
    totalPrice: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Payment QR Code",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Show this QR code to the seller",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // QR Code
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Payment QR Code",
                    modifier = Modifier.size(300.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Total amount
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "$totalPrice Caps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}
