package com.darkrockstudios.texteditor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.darkrockstudios.texteditor.TextOffset

class TextEditorSelectionManager(
	private val state: TextEditorState
) {
	var selection by mutableStateOf<TextSelection?>(null)
		private set

	fun updateSelection(start: TextOffset, end: TextOffset) {
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
			start = TextOffset(0, 0),
			end = TextOffset(lastLineIndex, lastLineLength)
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

	fun deleteSelection(text: String) {
		replaceSelection("")
	}

	fun replaceSelection(text: String) {
		val selection = selection ?: return

		val newLines = text.split('\n')

		when {
			// Single line selection replacement
			selection.isSingleLine() -> {
				val line = state.textLines[selection.start.line]
				state.replaceLine(
					selection.start.line,
					line.substring(0, selection.start.char) +
							text +
							line.substring(selection.end.char)
				)

				// Calculate new cursor position
				val newPosition = when {
					// Multi-line replacement text
					newLines.size > 1 -> {
						val lastLine = selection.start.line + newLines.size - 1
						TextOffset(lastLine, newLines.last().length)
					}
					// Single line replacement text
					else -> TextOffset(
						selection.start.line,
						selection.start.char + text.length
					)
				}

				state.updateCursorPosition(newPosition)
			}
			// Multi-line selection replacement
			else -> {
				// Handle first line
				val firstLine = state.textLines[selection.start.line]
				val startText = firstLine.substring(0, selection.start.char)

				// Handle last line
				val lastLine = state.textLines[selection.end.line]
				val endText = lastLine.substring(selection.end.char)

				// Remove all lines in the selection
				state.removeLines(selection.start.line, selection.end.line - selection.start.line + 1)

				// Insert new lines
				when (newLines.size) {
					0 -> {
						// Empty replacement
						state.insertLine(selection.start.line, startText + endText)
						state.updateCursorPosition(TextOffset(selection.start.line, startText.length))
					}
					1 -> {
						// Single line replacement
						state.insertLine(selection.start.line, startText + newLines[0] + endText)
						state.updateCursorPosition(TextOffset(
							selection.start.line,
							startText.length + newLines[0].length
						))
					}
					else -> {
						// Multi-line replacement
						// First line
						state.insertLine(selection.start.line, startText + newLines[0])

						// Middle lines
						for (i in 1 until newLines.size - 1) {
							state.insertLine(selection.start.line + i, newLines[i])
						}

						// Last line
						val lastIndex = selection.start.line + newLines.size - 1
						state.insertLine(lastIndex, newLines.last() + endText)

						state.updateCursorPosition(TextOffset(lastIndex, newLines.last().length))
					}
				}
			}
		}

		clearSelection()
		state.notifyContentChanged()
	}

	private fun isBeforeInDocument(a: TextOffset, b: TextOffset): Boolean {
		return when {
			a.line < b.line -> true
			a.line > b.line -> false
			else -> a.char < b.char
		}
	}
}