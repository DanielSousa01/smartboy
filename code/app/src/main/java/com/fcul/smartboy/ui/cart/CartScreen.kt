package com.fcul.smartboy.ui.cart

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.cart.Cart
import com.fcul.smartboy.ui.cart.components.CartItemCard
import androidx.compose.ui.platform.LocalContext
import com.fcul.smartboy.ui.cart.vm.CartError
import com.fcul.smartboy.ui.common.ErrorSnackbar

@Composable
fun CartScreen(
    currentCart: Cart?,
    isLoading: Boolean,
    qrCodeBitmap: Bitmap?,
    error: CartError?,
    onRemoveItem: (Long) -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onClearCart: () -> Unit,
    onGenerateQRCode: () -> Unit,
    onScanQRCode: () -> Unit,
    onDismissQRCode: () -> Unit,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current
    val errorMessage = when (error) {
        is CartError.CartEmpty -> context.getString(R.string.error_cart_empty)
        is CartError.ItemsUnavailable -> context.getString(R.string.error_cart_items_unavailable)
        is CartError.ProfileLoadFailed -> context.getString(R.string.error_profile_load_failed)
        is CartError.InsufficientCaps -> context.getString(
            R.string.error_cart_insufficient_caps,
            error.required,
            error.available
        )
        is CartError.ItemNotFound -> context.getString(R.string.error_cart_item_not_found)
        is CartError.ItemQuantityExceeded -> context.getString(
            R.string.error_cart_item_quantity_exceeded,
            error.itemName,
            error.available
        )
        is CartError.FailedToAddItem -> context.getString(R.string.error_cart_failed_to_add_item)
        is CartError.FailedToRemoveItem -> context.getString(R.string.error_cart_failed_to_remove_item)
        is CartError.FailedToUpdateQuantity -> context.getString(R.string.error_cart_failed_to_update_quantity)
        is CartError.FailedToClearCart -> context.getString(R.string.error_cart_failed_to_clear_cart)
        is CartError.FailedToGenerateQRCode -> context.getString(R.string.error_cart_failed_to_generate_qr_code)
        is CartError.FailedToCheckAvailability -> context.getString(R.string.error_failed_to_check_availability)
        is CartError.FailedToFetchAndAddItem -> context.getString(R.string.error_cart_failed_to_fetch_and_add_item)
        is CartError.Generic -> error.message ?: context.getString(R.string.error_generic)
        else -> null
    }

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
                        text = stringResource(R.string.cart_fail_load),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        ErrorSnackbar(
            errorMessage = errorMessage,
            onDismissError = onDismissError
        )
    }

    // QR Code Dialog
    qrCodeBitmap?.let { bitmap ->
        QRCodeDialog(
            bitmap = bitmap,
            totalPrice = currentCart?.totalPrice ?: 0,
            onDismiss = onDismissQRCode
        )
    }
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
                text = stringResource(R.string.cart),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (cart.items.isNotEmpty()) {
                IconButton(onClick = onClearCart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cart_clear)
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
            Text(stringResource(R.string.qr_scan_product))
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
                        text = stringResource(R.string.cart_empty),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.browse_marketplace),
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
                            text = "${cart.totalPrice} ${stringResource(R.string.caps)}",
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
                        Text(stringResource(R.string.qr_generate_payment))
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
                    text = stringResource(R.string.qr_payment),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.qr_show),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // QR Code
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.qr_payment),
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
                            text = "$totalPrice ${stringResource(R.string.caps)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }

    fun handleError() {

    }
}
