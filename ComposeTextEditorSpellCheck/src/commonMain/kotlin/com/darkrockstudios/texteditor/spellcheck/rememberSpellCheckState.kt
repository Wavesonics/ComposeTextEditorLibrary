package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.texteditor.state.rememberTextEditorState

@Composable
fun rememberSpellCheckState(
	spellChecker: SpellChecker?,
	initialText: AnnotatedString? = null
): SpellCheckState {
	val richTextState = rememberTextEditorState(initialText)
	val state = remember { SpellCheckState(richTextState, spellChecker) }

	// Run SpellCheck as soon as it is ready
	LaunchedEffect(spellChecker) {
		if (spellChecker != null) {
			state.spellChecker = spellChecker
			state.runFullSpellCheck()
		}
	}

	return state
}