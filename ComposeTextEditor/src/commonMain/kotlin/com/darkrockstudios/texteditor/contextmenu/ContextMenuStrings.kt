package com.darkrockstudios.texteditor.contextmenu

import androidx.compose.runtime.Composable
import com.darkrockstudios.texteditor.resources.Res
import com.darkrockstudios.texteditor.resources.context_menu_copy
import com.darkrockstudios.texteditor.resources.context_menu_cut
import com.darkrockstudios.texteditor.resources.context_menu_paste
import com.darkrockstudios.texteditor.resources.context_menu_select_all
import org.jetbrains.compose.resources.stringResource

/**
 * Localizable strings for the context menu.
 *
 * The recommended way to obtain an instance is [rememberDefaultContextMenuStrings],
 * which pulls labels from the bundled string resources and follows the host app's
 * locale. Pass a custom-constructed instance to override individual labels.
 */
data class ContextMenuStrings(
	val cut: String,
	val copy: String,
	val paste: String,
	val selectAll: String,
) {
	companion object {
		/**
		 * Fallback English defaults for callers that don't have a composable scope
		 * to resolve string resources (e.g. unit tests, eager construction). Prefer
		 * [rememberDefaultContextMenuStrings] in composable code so labels follow
		 * the active locale.
		 */
		val Default = ContextMenuStrings(
			cut = "Cut",
			copy = "Copy",
			paste = "Paste",
			selectAll = "Select All",
		)
	}
}

/**
 * Resolves [ContextMenuStrings] from the library's bundled string resources so
 * labels follow the host app's active locale. Add localized `strings.xml` files
 * under `composeResources/values-xx/` in this module to provide translations.
 */
@Composable
fun rememberDefaultContextMenuStrings(): ContextMenuStrings = ContextMenuStrings(
	cut = stringResource(Res.string.context_menu_cut),
	copy = stringResource(Res.string.context_menu_copy),
	paste = stringResource(Res.string.context_menu_paste),
	selectAll = stringResource(Res.string.context_menu_select_all),
)
