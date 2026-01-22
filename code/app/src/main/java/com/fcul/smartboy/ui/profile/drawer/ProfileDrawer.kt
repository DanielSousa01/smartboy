package com.fcul.smartboy.ui.profile.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fcul.smartboy.R

@Composable
fun ProfileDrawer(
    userName: String,
    onProfileClick: () -> Unit,
    onWalletClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    userPicture: String? = null,
    steps: Long = 0,
    radiation: Double = 0.0,
    radiationResistance: Double = 0.0,
    caps: Int = 0,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxHeight()
        ) {
            DrawerTop(
                userName = userName,
                userPicture = userPicture,
                onProfileClick = onProfileClick,
            )

            DrawerBody(
                steps = steps,
                radiation = radiation,
                radiationResistance = radiationResistance,
                caps = caps
            )

            DrawerFooter(
                onWalletClick = onWalletClick,
                onSettingsClick = onSettingsClick,
                onLogoutClick = onLogoutClick
            )
        }
    }
}

@Composable
fun DrawerTop(
    onProfileClick: () -> Unit,
    userName: String? = null,
    userPicture: String? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onProfileClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userPicture != null) {
                AsyncImage(
                    model = userPicture,
                    contentDescription = stringResource(R.string.profile_picture),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = stringResource(R.string.profile_icon),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = userName ?: stringResource(R.string.guest),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
    HorizontalDivider()
}

@Composable
fun DrawerBody(
    steps: Long,
    radiation: Double,
    radiationResistance: Double,
    caps: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Steps Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = stringResource(R.string.steps),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.steps),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = steps.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Radiation Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Waves,
                        contentDescription = stringResource(R.string.radiation_icon),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.radiation),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = "%.2f Sv".format(radiation),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Radiation Resistance Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = stringResource(R.string.radiation_resistance_icon),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.rad_resistance_short),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = "%.0f%%".format(radiationResistance * 100),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (radiationResistance > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.6f
                    )
                )
            }

            // Caps Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Paid,
                        contentDescription = stringResource(R.string.caps_icon),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.caps),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = caps.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun DrawerFooter(
    onWalletClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalDivider()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Wallet Button (Full Width)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .clickable { onWalletClick() }
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = stringResource(R.string.wallet_icon),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(stringResource(R.string.wallet))
        }

        // Settings and Logout Row
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable { onSettingsClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_icon),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.settings))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable { onLogoutClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = stringResource(R.string.logout_icon),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.logout))
            }
        }
    }
}