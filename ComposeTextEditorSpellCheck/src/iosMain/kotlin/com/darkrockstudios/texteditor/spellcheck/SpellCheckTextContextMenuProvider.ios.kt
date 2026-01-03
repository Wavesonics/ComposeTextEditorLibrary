package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.darkrockstudios.texteditor.spellcheck.ui.SpellCheckDropdown

/**
 * iOS implementation of spell check context menu.
 * Uses the same dropdown UI as Android and WASM since iOS doesn't have
 * a desktop-style context menu system in Compose Multiplatform.
 */
@Composable
actual fun SpellCheckTextContextMenuProvider(
	modifier: Modifier,
	spellCheckMenuState: SpellCheckMenuState,
	content: @Composable () -> Unit
) {
	Box(modifier = modifier) {
		content()

		spellCheckMenuState.missSpelling.value?.apply {
			SpellCheckDropdown(
				item,
				menuPosition,
				spellCheckMenuState.spellCheckState,
				dismiss = spellCheckMenuState::clearSpellCheck,
				correctSpelling = spellCheckMenuState::performCorrection
			)
		}
	}
}
