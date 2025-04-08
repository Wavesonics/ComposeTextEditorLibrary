package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.texteditor.TextEditorStyle
import com.darkrockstudios.texteditor.rememberTextEditorStyle
import com.darkrockstudios.texteditor.state.rememberTextEditorState

@Composable
fun rememberSpellCheckState(
	spellChecker: SpellChecker?,
	initialStyle: TextEditorStyle = rememberTextEditorStyle(),
	initialText: AnnotatedString? = null,
	enableSpellChecking: Boolean = true,
): SpellCheckState {
	val richTextState = rememberTextEditorState(initialStyle, initialText)
	val state = remember { SpellCheckState(richTextState, spellChecker, enableSpellChecking) }

	// Run SpellCheck as soon as it is ready
	LaunchedEffect(spellChecker) {
		if (spellChecker != null) {
			state.spellChecker = spellChecker
			state.runFullSpellCheck()
		}
	}

	return state
}