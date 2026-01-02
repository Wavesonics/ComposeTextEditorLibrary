package com.darkrockstudios.texteditor.input

import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * WebAssembly implementation of IME cursor synchronization.
 * On WASM, keyboard input is handled by the browser,
 * so no IME synchronization is needed from the Compose side.
 */
actual class ImeCursorSync actual constructor(
	private val state: TextEditorState
) {
	actual fun startSync(viewProvider: () -> Any?) {
		// No-op on WASM
	}

	actual fun stopSync() {
		// No-op on WASM
	}
}
