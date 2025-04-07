package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.texteditor.*
import com.darkrockstudios.texteditor.spellcheck.utils.debounceUntilQuiescentWithBatch
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SpellCheckingTextEditor(
	spellChecker: SpellChecker? = null,
	state: SpellCheckState = rememberSpellCheckState(spellChecker),
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	style: TextEditorStyle = rememberTextEditorStyle(),
	onRichSpanClick: RichSpanClickListener? = null,
) {
	val menuState by remember(state) { mutableStateOf(SpellCheckMenuState(state)) }
	val wordVisibilityBuffer = DpToPx(35.dp)

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
				enabled = enabled,
				style = style,
				onRichSpanClick = { span, type, offset ->
					return@BasicTextEditor if (type == SpanClickType.SECONDARY_CLICK || type == SpanClickType.TAP) {
						val correction = state.handleSpanClick(span)
						if (correction != null) {
							val menuPos = offset.copy(y = offset.y + wordVisibilityBuffer)
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
}

@Composable
private fun DpToPx(dp: Dp): Float {
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
