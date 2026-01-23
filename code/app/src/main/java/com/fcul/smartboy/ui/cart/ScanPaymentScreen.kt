package com.fcul.smartboy.ui.cart

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.fcul.smartboy.R
import com.fcul.smartboy.ui.common.QRCodeScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPaymentScreen(
    onCompletePurchase: suspend (String, String) -> Result<String>,
    onBackClick: () -> Unit,
    onPaymentComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showScanner by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payment_scan_qr)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showScanner) {
            QRCodeScanner(
                onQRCodeScanned = { qrData ->
                    // Prevent duplicate scans - ignore if already processing
                    if (isProcessing) {
                        return@QRCodeScanner
                    }

                    showScanner = false
                    isProcessing = true
                    errorMessage = null
                    successMessage = null

                    // Parse QR code: PAYMENT:BUYER_ID=xxx,SELLER_ID=yyy,TOTAL=zzz,ITEMS=n,TIMESTAMP=ttt
                    try {
                        val data = parsePaymentQR(qrData)

                        scope.launch {
                            val result = onCompletePurchase(
                                data.buyerId,
                                data.sellerId
                            )

                            result.onSuccess { message ->
                                successMessage = message
                                // Navigate back after 2 seconds
                                delay(2000)
                                onPaymentComplete()
                            }

                            result.onFailure { error ->
                                errorMessage = error.message ?: "Unknown error"
                            }

                            isProcessing = false
                        }
                    } catch (e: Exception) {
                        errorMessage = "Invalid QR code format"
                        isProcessing = false
                    }
                },
                onClose = {
                    showScanner = false
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when {
                        isProcessing -> {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.payment_processing))
                        }

                        successMessage != null -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.cd_success),
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = successMessage!!,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        errorMessage != null -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = stringResource(R.string.cd_error),
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = {
                                    errorMessage = null
                                    showScanner = true
                                }
                            ) {
                                Text(stringResource(R.string.payment_try_again))
                            }
                        }

                        else -> {
                            Text(
                                text = stringResource(R.string.payment_scan_qr_code),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showScanner = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.payment_scan_qr_code))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PaymentQRData(
    val buyerId: String,
    val sellerId: String,
    val total: Int,
    val items: Int,
    val timestamp: Long
)

private fun parsePaymentQR(qrData: String): PaymentQRData {
    // Expected format: PAYMENT:BUYER_ID=xxx,SELLER_ID=yyy,TOTAL=zzz,ITEMS=n,TIMESTAMP=ttt
    if (!qrData.startsWith("PAYMENT:")) {
        throw IllegalArgumentException("Invalid payment QR code")
    }

    val parts = qrData.removePrefix("PAYMENT:").split(",")
    val data = parts.associate {
        val (key, value) = it.split("=")
        key to value
    }

    return PaymentQRData(
        buyerId = data["BUYER_ID"] ?: throw IllegalArgumentException("Missing BUYER_ID"),
        sellerId = data["SELLER_ID"] ?: throw IllegalArgumentException("Missing SELLER_ID"),
        total = data["TOTAL"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid TOTAL"),
        items = data["ITEMS"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ITEMS"),
        timestamp = data["TIMESTAMP"]?.toLongOrNull() ?: throw IllegalArgumentException("Invalid TIMESTAMP")
    )
}
