package com.darkrockstudios.texteditor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.darkrockstudios.texteditor.CharLineOffset

class TextEditorSelectionManager(
	private val state: TextEditorState
) {
	var selection by mutableStateOf<TextSelection?>(null)
		private set

	fun updateSelection(start: CharLineOffset, end: CharLineOffset) {
		selection = if (start != end) {
			// Ensure start is always before end in the document
			if (isBeforeInDocument(start, end)) {
				TextSelection(start, end)
			} else {
				TextSelection(end, start)
			}
		} else {
			null
		}
	}

	fun clearSelection() {
		selection = null
	}

	fun selectAll() {
		if (state.textLines.isEmpty()) {
			clearSelection()
			return
		}

		val lastLineIndex = state.textLines.lastIndex
		val lastLineLength = state.textLines[lastLineIndex].length

		updateSelection(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(lastLineIndex, lastLineLength)
		)
	}

	fun getSelectedText(): String {
		val selection = selection ?: return ""

		return buildString {
			when {
				// Single line selection
				selection.isSingleLine() -> {
					append(state.textLines[selection.start.line].substring(
						selection.start.char,
						selection.end.char
					))
				}
				// Multi-line selection
				else -> {
					// First line - from selection start to end of line
					append(state.textLines[selection.start.line].substring(selection.start.char))
					append('\n')

					// Middle lines - entire lines
					for (line in (selection.start.line + 1) until selection.end.line) {
						append(state.textLines[line])
						append('\n')
					}

					// Last line - from start of line to selection end
					append(state.textLines[selection.end.line].substring(0, selection.end.char))
				}
			}
		}
	}

	fun deleteSelection() {
		val selection = selection ?: return
		state.delete(selection.range)
		clearSelection()
	}

	private fun isBeforeInDocument(a: CharLineOffset, b: CharLineOffset): Boolean {
		return when {
			a.line < b.line -> true
			a.line > b.line -> false
			else -> a.char < b.char
		}
	}
}