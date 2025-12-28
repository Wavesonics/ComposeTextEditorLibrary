package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.darkrockstudios.texteditor.spellcheck.api.Correction
import com.darkrockstudios.texteditor.state.WordSegment

@Composable
expect fun SpellCheckTextContextMenuProvider(
	modifier: Modifier = Modifier,
	spellCheckMenuState: SpellCheckMenuState,
	content: @Composable () -> Unit
)

/**
 * Represents a spell check item that can be either a word-level misspelling or a sentence-level correction.
 */
sealed class SpellCheckItem {
	data class MisspelledWord(val segment: WordSegment) : SpellCheckItem()
	data class SentenceIssue(val correction: Correction) : SpellCheckItem()
}

data class SpellCheckMenuState(
	val spellCheckState: SpellCheckState,
) {
	val missSpelling: MutableState<MissSpelling?> = mutableStateOf(null)

	fun clearSpellCheck() {
		missSpelling.value = null
	}

	fun performCorrection(item: SpellCheckItem, correctionText: String) {
		when (item) {
			is SpellCheckItem.MisspelledWord -> {
				spellCheckState.correctSpelling(item.segment, correctionText)
			}

			is SpellCheckItem.SentenceIssue -> {
				spellCheckState.applySentenceCorrection(item.correction, correctionText)
			}
		}
		clearSpellCheck()
	}

	/**
	 * Legacy method for backward compatibility with word-level corrections.
	 */
	fun performWordCorrection(toReplace: WordSegment, correction: String) {
		spellCheckState.correctSpelling(toReplace, correction)
		clearSpellCheck()
	}

	data class MissSpelling(val item: SpellCheckItem, val menuPosition: Offset)
}