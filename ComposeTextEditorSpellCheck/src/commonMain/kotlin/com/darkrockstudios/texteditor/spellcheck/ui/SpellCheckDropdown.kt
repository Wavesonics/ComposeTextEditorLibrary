package com.darkrockstudios.texteditor.spellcheck.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.darkrockstudios.texteditor.spellcheck.SpellCheckItem
import com.darkrockstudios.texteditor.spellcheck.SpellCheckState
import com.darkrockstudios.texteditor.spellcheck.api.Suggestion
import kotlin.math.roundToInt

@Composable
internal fun SpellCheckDropdown(
	item: SpellCheckItem?,
	position: Offset,
	spellCheckState: SpellCheckState,
	dismiss: () -> Unit,
	correctSpelling: (SpellCheckItem, String) -> Unit
) {
	var suggestionItems by remember { mutableStateOf(emptyList<Suggestion>()) }

	LaunchedEffect(item, spellCheckState) {
		item ?: return@LaunchedEffect
		suggestionItems = when (item) {
			is SpellCheckItem.MisspelledWord -> spellCheckState.getSuggestions(item.segment.text)
			is SpellCheckItem.SentenceIssue -> item.correction.suggestions
		}
	}

	Box(modifier = Modifier.offset {
		IntOffset(
			position.x.roundToInt(),
			position.y.roundToInt()
		)
	}) {
		DropdownMenu(
			expanded = item != null,
			onDismissRequest = dismiss,
		) {
			item ?: return@DropdownMenu
			if (suggestionItems.isNotEmpty()) {
				suggestionItems.forEach { suggestion ->
					DropdownMenuItem(
						text = { Text(suggestion.term) },
						onClick = { correctSpelling(item, suggestion.term) },
					)
				}
			} else {
				DropdownMenuItem(
					text = { Text("No suggestions") },
					onClick = dismiss,
				)
			}
		}
	}
}
