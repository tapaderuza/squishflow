package com.example.squishfocus

import androidx.compose.runtime.Composable

@Composable
expect fun BurpeeCameraVerifier(
    onVerified: () -> Unit,
    onCancel: () -> Unit,
)
