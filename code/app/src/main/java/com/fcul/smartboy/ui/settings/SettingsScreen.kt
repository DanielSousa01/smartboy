package com.fcul.smartboy.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.user.MeasurementUnit
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.ui.common.ErrorSnackbar
import com.fcul.smartboy.ui.profile.vm.ProfileError

@Composable
fun SettingsScreen(
    profile: Profile?,
    error: ProfileError?,
    onUpdateMeasurementUnit: (MeasurementUnit) -> Unit,
    onBackClick: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUnit = profile?.preferences?.measurementUnit ?: MeasurementUnit.METRIC

    val errorMessage = when (error) {
        is ProfileError.FailedToUpdateProfile -> stringResource(R.string.error_profile_update_preferences)
        else -> stringResource(R.string.error_generic)
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = modifier.padding(padding)) {
            // Preferences Section Header
            Text(
                text = stringResource(R.string.preferences),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Measurement Unit Setting
            Text(
                text = stringResource(R.string.measurement_unit),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Metric Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdateMeasurementUnit(MeasurementUnit.METRIC) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentUnit == MeasurementUnit.METRIC,
                    onClick = { onUpdateMeasurementUnit(MeasurementUnit.METRIC) }
                )
                Text(
                    text = stringResource(R.string.metric),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Imperial Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdateMeasurementUnit(MeasurementUnit.IMPERIAL) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentUnit == MeasurementUnit.IMPERIAL,
                    onClick = { onUpdateMeasurementUnit(MeasurementUnit.IMPERIAL) }
                )
                Text(
                    text = stringResource(R.string.imperial),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        ErrorSnackbar(
            errorMessage = errorMessage,
            onDismissError = onDismissError
        )
    }
}

