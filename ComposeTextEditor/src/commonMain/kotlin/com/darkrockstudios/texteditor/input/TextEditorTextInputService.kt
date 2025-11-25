package com.darkrockstudios.texteditor.input

import androidx.compose.ui.platform.PlatformTextInputSession
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Platform-specific text input service that handles IME integration.
 * On Android, this creates a PlatformTextInputMethodRequest with InputConnection.
 * On Desktop/WASM, this is a no-op since keyboard input arrives via KEY_TYPED events.
 */
expect class TextEditorTextInputService(state: TextEditorState) {
	/**
	 * Starts the platform-specific input method.
	 * On Android: Opens the soft keyboard and establishes InputConnection
	 * On Desktop/WASM: No-op (suspends indefinitely)
	 *
	 * This function never returns normally - it suspends until cancelled.
	 */
	suspend fun startInput(session: PlatformTextInputSession): Nothing
}
