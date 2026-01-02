package com.darkrockstudios.texteditor.input

import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Platform-specific IME cursor synchronization.
 * On Android, this notifies the InputMethodManager when cursor/selection changes.
 * On other platforms, this is a no-op.
 */
expect class ImeCursorSync(state: TextEditorState) {
	/**
	 * Start observing cursor changes and syncing to IME.
	 * Should be called when the text input session starts.
	 * @param viewProvider A function that returns the platform view (Android View on Android, null elsewhere)
	 */
	fun startSync(viewProvider: () -> Any?)

	/**
	 * Stop observing cursor changes.
	 * Should be called when the text input session ends.
	 */
	fun stopSync()
}
