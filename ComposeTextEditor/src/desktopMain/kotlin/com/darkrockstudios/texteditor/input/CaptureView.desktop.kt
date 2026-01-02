package com.darkrockstudios.texteditor.input

import androidx.compose.runtime.Composable

/**
 * Desktop implementation - no-op since there's no Android View to capture.
 */
@Composable
actual fun CaptureViewForIme() {
	// No-op on desktop
}
