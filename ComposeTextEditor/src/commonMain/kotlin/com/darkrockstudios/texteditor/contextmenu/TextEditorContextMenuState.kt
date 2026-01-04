package com.darkrockstudios.texteditor.contextmenu

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset

/**
 * Represents a custom menu item that can be added to the context menu.
 */
data class ContextMenuItem(
	val label: String,
	val enabled: Boolean = true,
	val onClick: () -> Unit
)

/**
 * State holder for the text editor context menu.
 * Tracks whether the menu is visible, its position, and any extra menu items.
 */
class TextEditorContextMenuState {
	/**
	 * The position where the menu should be displayed, or null if hidden.
	 */
	val menuPosition: MutableState<Offset?> = mutableStateOf(null)

	/**
	 * Extra menu items to display before the standard items (Cut, Copy, Paste, Select All).
	 * These are rendered first, followed by a divider if non-empty.
	 */
	val extraItems: MutableState<List<ContextMenuItem>> = mutableStateOf(emptyList())

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
	 * Show the context menu at the specified position with extra items.
	 */
	fun showMenu(position: Offset, items: List<ContextMenuItem>) {
		extraItems.value = items
		menuPosition.value = position
	}

	/**
	 * Dismiss the context menu and clear extra items.
	 */
	fun dismissMenu() {
		menuPosition.value = null
		extraItems.value = emptyList()
	}
}
