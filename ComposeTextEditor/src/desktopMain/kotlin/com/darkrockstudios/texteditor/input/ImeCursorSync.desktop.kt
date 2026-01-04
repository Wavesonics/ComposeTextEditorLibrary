package com.darkrockstudios.texteditor.input

import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Desktop implementation of IME cursor synchronization.
 * On desktop, keyboard input is handled directly via key events,
 * so no IME synchronization is needed.
 */
@Suppress("UNUSED_PARAMETER")
actual class ImeCursorSync actual constructor(
	private val state: TextEditorState
) {
	actual fun startSync() {
		// No-op on desktop
	}

	actual fun stopSync() {
		// No-op on desktop
	}
}
