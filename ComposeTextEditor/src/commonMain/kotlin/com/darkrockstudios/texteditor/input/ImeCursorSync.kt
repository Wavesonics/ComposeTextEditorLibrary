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
	 * On Android, the View is obtained from state.platformExtensions.view.
	 */
	fun startSync()

	/**
	 * Stop observing cursor changes.
	 * Should be called when the text input session ends.
	 */
	fun stopSync()
}
