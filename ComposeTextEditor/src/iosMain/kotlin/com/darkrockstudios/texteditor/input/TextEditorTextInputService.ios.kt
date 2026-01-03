package com.darkrockstudios.texteditor.input

import androidx.compose.ui.platform.PlatformTextInputSession
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.coroutines.awaitCancellation

/**
 * iOS implementation of TextEditorTextInputService.
 *
 * iOS keyboard input is managed by Compose Multiplatform's iOS integration,
 * which handles UITextInput protocols internally.
 *
 * Note: Complex IME features like Japanese input may need additional work.
 * This implementation suspends indefinitely, allowing keyboard events to flow
 * through Compose's standard input handling.
 */
actual class TextEditorTextInputService actual constructor(
	private val state: TextEditorState
) {
	actual suspend fun startInput(session: PlatformTextInputSession): Nothing {
		// iOS keyboard events are handled by Compose's iOS backend
		// Just suspend indefinitely for now
		awaitCancellation()
	}
}
