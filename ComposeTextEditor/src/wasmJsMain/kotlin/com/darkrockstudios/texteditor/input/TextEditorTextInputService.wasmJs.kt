package com.darkrockstudios.texteditor.input

import androidx.compose.ui.platform.PlatformTextInputSession
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.coroutines.awaitCancellation

/**
 * WASM implementation of TextEditorTextInputService.
 * WASM/browser uses browser keyboard events which are handled through
 * KeyInputModifierNode, so no platform IME integration is needed here.
 */
actual class TextEditorTextInputService actual constructor(
	private val state: TextEditorState
) {
	actual suspend fun startInput(session: PlatformTextInputSession): Nothing {
		// WASM uses browser keyboard events
		// No platform IME integration needed - just suspend indefinitely
		awaitCancellation()
	}
}
