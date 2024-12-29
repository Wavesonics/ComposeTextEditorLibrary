package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.darkrockstudios.texteditor.state.WordSegment

@Composable
expect fun SpellCheckTextContextMenuProvider(
	modifier: Modifier = Modifier,
	spellCheckMenuState: SpellCheckMenuState,
	content: @Composable () -> Unit
)

data class SpellCheckMenuState(
	val spellCheckState: SpellCheckState,
) {
	val missSpelling: MutableState<MissSpelling?> = mutableStateOf(null)

	fun clearSpellCheck() {
		missSpelling.value = null
	}

	fun performCorrection(toReplace: WordSegment, correction: String) {
		spellCheckState.correctSpelling(toReplace, correction)
		clearSpellCheck()
	}

	data class MissSpelling(val wordSegment: WordSegment, val menuPosition: Offset)
}