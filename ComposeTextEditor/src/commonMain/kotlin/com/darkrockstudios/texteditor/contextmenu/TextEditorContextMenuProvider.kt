package com.darkrockstudios.texteditor.contextmenu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific context menu provider for the text editor.
 * Wraps content and displays the context menu when triggered.
 *
 * @param modifier Modifier for the container
 * @param menuState State controlling menu visibility and position
 * @param actions Actions to perform when menu items are selected
 * @param strings Localizable strings for menu items
 * @param enabled Whether editing operations (cut, paste) are enabled
 * @param content The content to wrap (the text editor)
 */
@Composable
expect fun TextEditorContextMenuProvider(
	modifier: Modifier = Modifier,
	menuState: TextEditorContextMenuState,
	actions: ContextMenuActions,
	strings: ContextMenuStrings,
	enabled: Boolean,
	content: @Composable () -> Unit
)
