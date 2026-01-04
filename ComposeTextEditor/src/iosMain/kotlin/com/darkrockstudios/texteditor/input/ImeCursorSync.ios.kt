package com.darkrockstudios.texteditor.input

import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * iOS implementation of IME cursor synchronization.
 * On iOS, keyboard input is handled by Compose Multiplatform's iOS backend,
 * so no explicit IME synchronization is needed from the Compose side.
 */
@Suppress("UNUSED_PARAMETER")
actual class ImeCursorSync actual constructor(
	private val state: TextEditorState
) {
	actual fun startSync() {
		// No-op on iOS
	}

	actual fun stopSync() {
		// No-op on iOS
	}
}
