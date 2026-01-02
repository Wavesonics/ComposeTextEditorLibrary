package com.darkrockstudios.texteditor.input

import androidx.compose.runtime.Composable

/**
 * Platform-specific composable that captures the current view for IME synchronization.
 * On Android, this stores the View from LocalView for use by the IME cursor sync.
 * On other platforms, this is a no-op.
 */
@Composable
expect fun CaptureViewForIme()
