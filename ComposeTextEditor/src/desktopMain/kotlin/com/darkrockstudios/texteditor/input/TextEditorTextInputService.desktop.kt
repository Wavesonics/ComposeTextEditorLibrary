package com.darkrockstudios.texteditor.input

import androidx.compose.ui.platform.PlatformTextInputSession
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.coroutines.awaitCancellation

/**
 * Desktop implementation of TextEditorTextInputService.
 *
 * Desktop uses KEY_TYPED events through KeyInputModifierNode for character input.
 *
 * Note: Windows emoji picker (Win+.) and some IME features may not work with this
 * custom text editor. This is a known limitation because Compose Desktop's
 * PlatformTextInputMethodRequest interface requires internal types that are
 * difficult to implement externally. Standard keyboard input (including Unicode
 * characters typed directly) should work correctly via KEY_TYPED events.
 */
actual class TextEditorTextInputService actual constructor(
	private val state: TextEditorState
) {
	actual suspend fun startInput(session: PlatformTextInputSession): Nothing {
		// Desktop uses KEY_TYPED events through KeyInputModifierNode
		// IME features like emoji picker require internal Compose types to implement
		// Just suspend indefinitely for now
		awaitCancellation()
	}
}
