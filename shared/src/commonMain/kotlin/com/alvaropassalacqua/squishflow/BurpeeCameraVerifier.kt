package com.alvaropassalacqua.squishflow

import androidx.compose.runtime.Composable

@Composable
expect fun BurpeeCameraVerifier(
    onVerified: () -> Unit,
    onCancel: () -> Unit,
)
