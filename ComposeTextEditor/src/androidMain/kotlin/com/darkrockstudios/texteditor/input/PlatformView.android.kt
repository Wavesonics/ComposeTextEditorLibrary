package com.darkrockstudios.texteditor.input

import android.view.View

/**
 * Stores the current Android View for IME synchronization.
 * This is set by the composable when it's composed and used by the modifier node.
 */
internal object AndroidViewHolder {
	var currentView: View? = null
}

/**
 * Android implementation - gets the Android View from the stored holder.
 * The View is stored when the composable is created.
 */
actual fun androidx.compose.ui.Modifier.Node.getPlatformView(): Any? {
	return AndroidViewHolder.currentView
}
