package com.darkrockstudios.texteditor.contextmenu

/**
 * Localizable strings for the context menu.
 * Provide a custom implementation to localize the context menu UI.
 */
data class ContextMenuStrings(
	val cut: String,
	val copy: String,
	val paste: String,
	val selectAll: String,
) {
	companion object {
		/**
		 * Default English strings for the context menu.
		 */
		val Default = ContextMenuStrings(
			cut = "Cut",
			copy = "Copy",
			paste = "Paste",
			selectAll = "Select All",
		)
	}
}
