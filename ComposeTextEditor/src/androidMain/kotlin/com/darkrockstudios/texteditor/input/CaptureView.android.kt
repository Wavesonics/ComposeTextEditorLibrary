package com.darkrockstudios.texteditor.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Android implementation - captures the current View from LocalView.
 * This stores the View so it can be used by ImeCursorSync for updateSelection calls.
 */
@Composable
actual fun CaptureViewForIme() {
	val view = LocalView.current

	DisposableEffect(view) {
		AndroidViewHolder.currentView = view
		onDispose {
			// Only clear if it's still our view
			if (AndroidViewHolder.currentView === view) {
				AndroidViewHolder.currentView = null
			}
		}
	}
}
