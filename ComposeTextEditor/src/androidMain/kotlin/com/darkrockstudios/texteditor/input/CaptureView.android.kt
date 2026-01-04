package com.darkrockstudios.texteditor.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Android implementation - captures the current View from LocalView.
 * This stores the View in the state's platformExtensions so it can be used
 * by ImeCursorSync for updateSelection calls and cursor anchor info.
 */
@Composable
actual fun CaptureViewForIme(state: TextEditorState) {
	val view = LocalView.current

	DisposableEffect(view, state) {
		state.platformExtensions.view = view
		onDispose {
			// Only clear if it's still our view
			if (state.platformExtensions.view === view) {
				state.platformExtensions.view = null
			}
		}
	}
}
