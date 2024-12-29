package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.foundation.ContextMenuData
import androidx.compose.foundation.LocalContextMenuData
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.darkrockstudios.texteditor.spellcheck.ui.SpellCheckDropdown

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
				wordSegment,
				menuPosition,
				spellCheckMenuState.spellCheckState,
				dismiss = spellCheckMenuState::clearSpellCheck,
				correctSpelling = spellCheckMenuState::performCorrection
			)
		}
	}
}

//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//actual fun SpellCheckTextContextMenuProvider(
//    modifier: Modifier,
//    spellCheckMenuState: SpellCheckMenuState,
//    content: @Composable () -> Unit
//) {
//    val currentContextMenuData = LocalContextMenuData.current
//    val contextMenuData = remember(spellCheckMenuState.missSpelling.value) {
//        ContextMenuData(
//            items = {
//                val wordSegment = spellCheckMenuState.missSpelling.value?.wordSegment
//                    ?: return@ContextMenuData emptyList()
//
//                val suggestionItems = spellCheckMenuState.spellCheckState.getSuggestions(wordSegment.text)
//                if (suggestionItems.isNotEmpty()) {
//                    suggestionItems.map { suggestion ->
//                        ContextMenuItem(
//                            suggestion.term,
//                            onClick = {
//                                spellCheckMenuState.performCorrection(
//                                    wordSegment,
//                                    suggestion.term
//                                )
//                            }
//                        )
//                    }
//                } else {
//                    emptyList()
//                }
//            },
//            next = currentContextMenuData
//        )
//    }
//
//    var menuState = remember { ContextMenuState() }
//    TextContextMenu.Default.Area(
//        textManager = TextContextMenu.Default.text,
//        state = menuState
//    ) {
//        LocalContextMenuDataProvider(contextMenuData) {
//            Box(modifier = modifier) {
//                TextContextMenuArea(TextContextMenu.Default) {
//
//                }
//                content()
//            }
//        }
//    }
//}

/**
 * In order to be able to recompose the menu items, I need this overload.
 */
@Composable
private fun LocalContextMenuDataProvider(
	data: ContextMenuData,
	content: @Composable () -> Unit
) {
	CompositionLocalProvider(
		LocalContextMenuData provides data
	) {
		content()
	}
}