package com.fcul.smartboy.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.R
import kotlinx.coroutines.delay

@Composable
fun ErrorSnackbar(
    errorMessage: String?,
    modifier: Modifier = Modifier,
    onDismissError: (() -> Unit)? = null,
    durationMillis: Long = 4000L, // Default to 4 seconds
) {
    var visible by remember(errorMessage) { mutableStateOf(true) }

    if (errorMessage != null && visible) {
        LaunchedEffect(errorMessage) {
            delay(durationMillis)
            visible = false
            onDismissError?.invoke()
        }
        Snackbar(
            modifier = modifier.padding(horizontal = 16.dp), // Less intrusive padding
            action = onDismissError?.let {
                {
                    TextButton(onClick = it) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            }
        ) {
            Text(errorMessage)
        }
    }
}