package com.darkrockstudios.texteditor.input

import androidx.compose.runtime.Composable
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * iOS implementation - no-op since there's no Android View to capture.
 * iOS keyboard integration is handled by Compose Multiplatform's iOS backend.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
actual fun CaptureViewForIme(state: TextEditorState) {
	// No-op on iOS
}
