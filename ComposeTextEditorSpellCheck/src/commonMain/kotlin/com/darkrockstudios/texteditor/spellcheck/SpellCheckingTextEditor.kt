package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.texteditor.RichSpanClickListener
import com.darkrockstudios.texteditor.TextEditor
import com.darkrockstudios.texteditor.state.SpanClickType
import com.mohamedrejeb.richeditor.compose.spellcheck.utils.debounceUntilQuiescent
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SpellCheckingTextEditor(
	spellChecker: SpellChecker? = null,
	state: SpellCheckState = rememberSpellCheckState(spellChecker),
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	onRichSpanClick: RichSpanClickListener? = null,
) {
	val menuState by remember(state) { mutableStateOf(SpellCheckMenuState(state)) }

	LaunchedEffect(state) {
		state.textState.editOperations.debounceUntilQuiescent(500.milliseconds)
			.collect { operation ->
				state.onTextChange(operation)
			}
	}

	SpellCheckTextContextMenuProvider(
		spellCheckMenuState = menuState,
	) {
		TextEditor(
			state = state.textState,
			modifier = modifier,
			enabled = enabled,
			onRichSpanClick = { span, type, offset ->
				return@TextEditor if (type == SpanClickType.SECONDARY_CLICK || type == SpanClickType.TAP) {
					val correction = state.handleSpanClick(span)
					if (correction != null) {
						val menuPos = offset.copy(y = offset.y - state.textState.scrollState.value)
						menuState.missSpelling.value =
							SpellCheckMenuState.MissSpelling(correction, menuPos)
						true
					} else {
						menuState.missSpelling.value = null
						onRichSpanClick?.invoke(span, type, offset) ?: false
					}
				} else {
					menuState.missSpelling.value = null
					false
				}
			},
		)
	}
}