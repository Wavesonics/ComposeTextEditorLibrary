package com.darkrockstudios.texteditor.input

import androidx.compose.ui.platform.PlatformTextInputSession
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.coroutines.awaitCancellation

/**
 * Desktop implementation of TextEditorTextInputService.
 * Desktop uses KEY_TYPED events through KeyInputModifierNode for character input,
 * so no platform IME integration is needed here.
 */
actual class TextEditorTextInputService actual constructor(
	private val state: TextEditorState
) {
	actual suspend fun startInput(session: PlatformTextInputSession): Nothing {
		// Desktop uses KEY_TYPED events through KeyInputModifierNode
		// No platform IME integration needed - just suspend indefinitely
		awaitCancellation()
	}
}
