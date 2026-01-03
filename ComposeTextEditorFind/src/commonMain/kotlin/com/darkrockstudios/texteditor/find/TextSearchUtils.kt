package com.darkrockstudios.texteditor.find

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Find all occurrences of a query string in the text editor.
 *
 * @param query The string to search for
 * @param caseSensitive Whether the search should be case-sensitive
 * @return List of TextEditorRange for each match, in document order
 */
fun TextEditorState.findAll(
	query: String,
	caseSensitive: Boolean = false
): List<TextEditorRange> {
	if (query.isEmpty()) return emptyList()

	val results = mutableListOf<TextEditorRange>()
	val searchQuery = if (caseSensitive) query else query.lowercase()

	textLines.forEachIndexed { lineIndex, annotatedString ->
		val lineText = if (caseSensitive) {
			annotatedString.text
		} else {
			annotatedString.text.lowercase()
		}

		var startIndex = 0
		while (true) {
			val foundIndex = lineText.indexOf(searchQuery, startIndex)
			if (foundIndex == -1) break

			val start = CharLineOffset(line = lineIndex, char = foundIndex)
			val end = CharLineOffset(line = lineIndex, char = foundIndex + query.length)
			results.add(TextEditorRange(start, end))

			startIndex = foundIndex + 1
		}
	}

	return results
}

/**
 * Find the match nearest to the current cursor position.
 * Returns the index in the matches list, or -1 if no matches.
 */
fun findNearestMatchIndex(
	matches: List<TextEditorRange>,
	cursorPosition: CharLineOffset
): Int {
	if (matches.isEmpty()) return -1

	// Find the first match at or after the cursor
	val indexAtOrAfter = matches.indexOfFirst { it.start >= cursorPosition }

	return when {
		indexAtOrAfter == -1 -> 0 // All matches are before cursor, wrap to first
		indexAtOrAfter == 0 -> 0 // First match is at or after cursor
		else -> {
			// Check if the match before is closer
			val matchBefore = matches[indexAtOrAfter - 1]
			val matchAtOrAfter = matches[indexAtOrAfter]

			// If cursor is within or at the start of matchBefore, use it
			if (cursorPosition >= matchBefore.start && cursorPosition <= matchBefore.end) {
				indexAtOrAfter - 1
			} else {
				indexAtOrAfter
			}
		}
	}
}
