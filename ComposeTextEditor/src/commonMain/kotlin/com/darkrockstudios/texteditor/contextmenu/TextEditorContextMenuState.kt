package com.darkrockstudios.texteditor.contextmenu

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset

/**
 * State holder for the text editor context menu.
 * Tracks whether the menu is visible and its position.
 */
class TextEditorContextMenuState {
	/**
	 * The position where the menu should be displayed, or null if hidden.
	 */
	val menuPosition: MutableState<Offset?> = mutableStateOf(null)

	/**
	 * Whether the menu is currently visible.
	 */
	val isVisible: Boolean
		get() = menuPosition.value != null

	/**
	 * Show the context menu at the specified position.
	 */
	fun showMenu(position: Offset) {
		menuPosition.value = position
	}

	/**
	 * Dismiss the context menu.
	 */
	fun dismissMenu() {
		menuPosition.value = null
	}
}
