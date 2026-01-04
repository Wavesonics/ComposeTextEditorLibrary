package com.darkrockstudios.texteditor.input

import androidx.compose.runtime.Composable
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * WASM implementation - no-op since there's no Android View to capture.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
actual fun CaptureViewForIme(state: TextEditorState) {
	// No-op on WASM
}
