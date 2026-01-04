package com.darkrockstudios.texteditor.spellcheck

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.*
import com.darkrockstudios.texteditor.contextmenu.ContextMenuItem
import com.darkrockstudios.texteditor.contextmenu.ContextMenuStrings
import com.darkrockstudios.texteditor.contextmenu.TextEditorContextMenuState
import com.darkrockstudios.texteditor.spellcheck.api.Correction
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker
import com.darkrockstudios.texteditor.spellcheck.utils.debounceUntilQuiescentWithBatch
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditOperation
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.WordSegment
import kotlinx.coroutines.launch
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
	contextMenuStrings: ContextMenuStrings = ContextMenuStrings.Default,
	onRichSpanClick: RichSpanClickListener? = null,
) {
	val contextMenuState = remember { TextEditorContextMenuState() }
	val wordVisibilityBuffer = dpToPx(35.dp)
	val coroutineScope = rememberCoroutineScope()

	// Track the current spell check item for right-click context
	var currentSpellCheckItem by remember { mutableStateOf<SpellCheckItem?>(null) }

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

	fun createSpellSuggestionItems(
		item: SpellCheckItem,
		suggestions: List<com.darkrockstudios.texteditor.spellcheck.api.Suggestion>
	): List<ContextMenuItem> {
		return if (suggestions.isNotEmpty()) {
			suggestions.map { suggestion ->
				ContextMenuItem(
					label = suggestion.term,
					enabled = true,
					onClick = {
						when (item) {
							is SpellCheckItem.MisspelledWord -> {
								state.correctSpelling(item.segment, suggestion.term)
							}

							is SpellCheckItem.SentenceIssue -> {
								state.applySentenceCorrection(item.correction, suggestion.term)
							}
						}
					}
				)
			}
		} else {
			listOf(
				ContextMenuItem(
					label = "No suggestions",
					enabled = false,
					onClick = { }
				)
			)
		}
	}

	fun showContextMenu(offset: Offset, spellCheckItem: SpellCheckItem?) {
		val menuPos = Offset(offset.x, offset.y + wordVisibilityBuffer)

		if (spellCheckItem == null) {
			// No spell check item - just show standard menu
			contextMenuState.showMenu(menuPos)
		} else {
			// Show menu immediately, then fetch suggestions async for misspelled words
			when (spellCheckItem) {
				is SpellCheckItem.MisspelledWord -> {
					// Show loading state
					contextMenuState.showMenu(
						menuPos, listOf(
							ContextMenuItem(label = "Loading...", enabled = false, onClick = {})
						)
					)
					// Fetch suggestions asynchronously
					coroutineScope.launch {
						val suggestions = state.getSuggestions(spellCheckItem.segment.text)
						val items = createSpellSuggestionItems(spellCheckItem, suggestions)
						contextMenuState.extraItems.value = items
					}
				}

				is SpellCheckItem.SentenceIssue -> {
					// Sentence issues already have suggestions
					val items = createSpellSuggestionItems(spellCheckItem, spellCheckItem.correction.suggestions)
					contextMenuState.showMenu(menuPos, items)
				}
			}
		}
	}

	Surface(modifier = modifier.focusBorder(state.textState.isFocused && enabled, style)) {
		BasicTextEditor(
			state = state.textState,
			modifier = Modifier,
			contentPadding = contentPadding,
			enabled = enabled,
			autoFocus = autoFocus,
			style = style,
			contextMenuStrings = contextMenuStrings,
			contextMenuState = contextMenuState,
			onRichSpanClick = { span, type, offset ->
				if (type == SpanClickType.SECONDARY_CLICK || type == SpanClickType.TAP) {
					// Check if this span is a spell check span
					val spellCheckItem: SpellCheckItem? = when (val clickResult = state.handleSpanClick(span)) {
						is WordSegment -> SpellCheckItem.MisspelledWord(clickResult)
						is Correction -> SpellCheckItem.SentenceIssue(clickResult)
						else -> null
					}
					// Store the spell check item for context menu
					currentSpellCheckItem = spellCheckItem
					// Show context menu with spell suggestions
					showContextMenu(offset, spellCheckItem)
					// Return true to indicate we handled it
					true
				} else {
					currentSpellCheckItem = null
					onRichSpanClick?.invoke(span, type, offset) ?: false
				}
			},
		)
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
