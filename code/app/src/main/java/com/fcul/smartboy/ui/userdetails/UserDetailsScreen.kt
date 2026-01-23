package com.fcul.smartboy.ui.userdetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.ui.common.ErrorSnackbar
import com.fcul.smartboy.ui.profile.vm.ProfileError
import com.fcul.smartboy.utils.MeasurementUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(
    profile: Profile?,
    sellingItems: List<SellingItem>,
    isLoading: Boolean,
    error: ProfileError?,
    onSendMessage: (String) -> Unit,
    onBackClick: () -> Unit,
    onDismissError: () -> Unit,
) {
    val errorMessage = when (error) {
        is ProfileError.FailedToLoadProfile -> stringResource(R.string.error_profile_failed_to_load)
        is ProfileError.FailedToLoadSellingItems -> stringResource(R.string.error_profile_failed_to_load_selling_items)
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_details)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
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

                profile != null -> {
                    UserDetailsContent(
                        profile = profile,
                        sellingItems = sellingItems,
                        onSendMessage = { onSendMessage(profile.userId) }
                    )
                }
            }
        }

        ErrorSnackbar(
            errorMessage = errorMessage,
            onDismissError = onDismissError
        )
    }
}

@Composable
fun UserDetailsContent(
    profile: Profile,
    sellingItems: List<SellingItem>,
    onSendMessage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User Level (calculated from steps)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = profile.username,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(R.string.level),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = calculateLevel(profile.steps).toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Stats
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    StatRow(
                        label = stringResource(R.string.steps),
                        value = profile.steps.toString()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(
                        label = stringResource(R.string.distance_traveled),
                        value = MeasurementUtils.formatDistance(
                            profile.distance,
                            profile.preferences.measurementUnit
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(
                        label = stringResource(R.string.caps),
                        value = profile.caps.toString()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(
                        label = stringResource(R.string.rad_exposure),
                        value = String.format(Locale.US, "%.2f Sv", profile.radiation)
                    )
                }
            }
        }

        // Selling Inventory Section
        item {
            Text(
                text = "Selling Inventory",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        if (sellingItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items for sale",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sellingItems, key = { it.id }) { item ->
                SellingItemCard(item)
            }
        }

        // Send Message Button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSendMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.send_message))
            }
        }
    }
}

@Composable
fun SellingItemCard(item: SellingItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${item.valuePerUnit} Caps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Qty: ${item.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

fun calculateLevel(steps: Long): Int {
    // Simple level calculation: 1 level per 1000 steps
    return (steps / 1000).toInt() + 1
}
