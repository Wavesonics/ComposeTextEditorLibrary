package com.darkrockstudios.texteditor.input

import androidx.compose.runtime.Composable

/**
 * WASM implementation - no-op since there's no Android View to capture.
 */
@Composable
actual fun CaptureViewForIme() {
	// No-op on WASM
}
