package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.*
import com.darkrockstudios.texteditor.spellcheck.api.Correction
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker
import com.darkrockstudios.texteditor.spellcheck.utils.debounceUntilQuiescentWithBatch
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.WordSegment
import kotlin.time.Duration.Companion.milliseconds

private val DefaultContentPadding = PaddingValues(start = 8.dp)

@Composable
fun SpellCheckingTextEditor(
	spellChecker: EditorSpellChecker? = null,
	state: SpellCheckState = rememberSpellCheckState(spellChecker),
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = DefaultContentPadding,
	enabled: Boolean = true,
	autoFocus: Boolean = false,
	style: TextEditorStyle = rememberTextEditorStyle(),
	onRichSpanClick: RichSpanClickListener? = null,
) {
	val menuState by remember(state) { mutableStateOf(SpellCheckMenuState(state)) }
	val wordVisibilityBuffer = dpToPx(35.dp)

	LaunchedEffect(state) {
		state.textState.editOperations
			.collect { operation ->
				state.invalidateSpellCheckSpans(operation)
			}
	}

	LaunchedEffect(state) {
		state.textState.editOperations.debounceUntilQuiescentWithBatch(500.milliseconds)
			.collect { operations ->
				val rangesToCheck = computeAffectedRanges(operations, state.textState)
				rangesToCheck.forEach { range ->
					state.runPartialSpellCheck(range)
				}
			}
	}

	Surface(modifier = modifier.focusBorder(state.textState.isFocused && enabled, style)) {
		SpellCheckTextContextMenuProvider(
			spellCheckMenuState = menuState,
		) {
			BasicTextEditor(
				state = state.textState,
				modifier = Modifier,
				contentPadding = contentPadding,
				enabled = enabled,
				autoFocus = autoFocus,
				style = style,
				onRichSpanClick = { span, type, offset ->
					return@BasicTextEditor if (type == SpanClickType.SECONDARY_CLICK || type == SpanClickType.TAP) {
						val spellCheckItem: SpellCheckItem? = when (val clickResult = state.handleSpanClick(span)) {
							is WordSegment -> SpellCheckItem.MisspelledWord(clickResult)
							is Correction -> SpellCheckItem.SentenceIssue(clickResult)
							else -> null
						}
						if (spellCheckItem != null) {
							val menuPos = offset.copy(y = offset.y + wordVisibilityBuffer)
							menuState.missSpelling.value =
								SpellCheckMenuState.MissSpelling(spellCheckItem, menuPos)
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
}

@Composable
private fun dpToPx(dp: Dp): Float {
	val density = LocalDensity.current.density
	return dp.value * density
}

private fun computeAffectedRanges(
	operations: List<TextEditOperation>,
	state: TextEditorState
): List<TextEditorRange> {
	return operations.fold(mutableListOf<TextEditorRange>()) { ranges, op ->
		val opRange = when (op) {
			is TextEditOperation.Insert -> TextEditorRange(
				op.position,
				op.position.copy(char = op.position.char + op.text.length)
			)

			is TextEditOperation.Delete -> op.range
			is TextEditOperation.Replace -> op.range
			else -> null
		}
		opRange?.let { newRange ->
			// Merge overlapping ranges
			val overlapping = ranges.filter { it.intersects(newRange) }
			ranges.removeAll(overlapping)
			val mergedRange = overlapping.fold(newRange) { acc, r -> acc.merge(r) }
			ranges.add(mergedRange)
		}
		ranges
	}
}
