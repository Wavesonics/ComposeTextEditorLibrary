package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange

fun TextEditorState.wordSegments(): Sequence<WordSegment> = sequence {
	textLines.asSequence().withIndex().forEach { (lineIndex, line) ->
		var wordStart = -1
		var currentChar = 0

		val text = line.text

		while (currentChar <= text.length) {
			val char = if (currentChar < text.length) text[currentChar] else ' '

			when {
				// Start of a new word
				wordStart == -1 && isWordStartChar(char) -> {
					wordStart = currentChar
				}

				// End of a word
				wordStart != -1 && (currentChar == text.length || !isWordChar(
					text,
					currentChar
				)) -> {
					yield(
						WordSegment(
							text = text.substring(wordStart, currentChar),
							range = TextEditorRange(
								start = CharLineOffset(lineIndex, wordStart),
								end = CharLineOffset(lineIndex, currentChar)
							)
						)
					)
					wordStart = -1
				}
			}
			currentChar++
		}
	}
}

private fun isWordStartChar(char: Char): Boolean {
	return char.isLetterOrDigit() || char == '_'
}

internal fun isWordChar(text: CharSequence, pos: Int): Boolean {
	// First check if position is valid
	if (pos < 0 || pos >= text.length) {
		return false
	}

	val char = text[pos]

	return when {
		// Basic word characters
		char.isLetterOrDigit() || char == '_' -> true

		// Special characters that might be part of a word
		char == '\'' -> {
			hasLetterAt(text, pos - 1) && hasLetterAt(text, pos + 1)
		}

		char == '.' -> {
			hasLetterAt(text, pos - 1) && (
					hasLetterAt(text, pos + 1) ||  // Middle of abbreviation
							(pos > 1 && text.getOrNull(pos - 2) == '.')  // End of abbreviation
					)
		}

		char == '-' -> {
			hasLetterAt(text, pos - 1) && hasLetterAt(text, pos + 1)
		}

		// Handle letters after apostrophes or periods
		char.isLetter() -> {
			hasApostropheOrPeriodAt(text, pos - 1)
		}

		else -> false
	}
}

private fun hasLetterAt(text: CharSequence, pos: Int): Boolean {
	return text.getOrNull(pos)?.isLetter() == true
}

private fun hasApostropheOrPeriodAt(text: CharSequence, pos: Int): Boolean {
	return when (text.getOrNull(pos)) {
		'\'' -> true
		'.' -> true
		else -> false
	}
}

/**
 * Finds the WordSegment at a given position, if one exists.
 * A WordSegment is considered to exist at a position if the position is within
 * or immediately adjacent to a word.
 *
 * @param position The position to check for a word
 * @return WordSegment if a word exists at or adjacent to the position, null otherwise
 */
fun TextEditorState.findWordSegmentAt(position: CharLineOffset): WordSegment? {
	// Validate position is within bounds
	if (position.line < 0 || position.line >= textLines.size) {
		return null
	}

	val line = textLines[position.line].text
	if (line.isEmpty()) {
		return null
	}

	// If we're at the end of the line, step back one character
	val adjustedChar = if (position.char >= line.length) {
		if (position.char == line.length && position.char > 0) {
			position.char - 1
		} else {
			return null
		}
	} else {
		position.char
	}

	// Start looking from our position
	var start = adjustedChar
	var end = adjustedChar

	// If we're not in a word character, this might be a boundary position
	if (!isWordChar(line, adjustedChar)) {
		// Check if we're at the end of a word
		if (adjustedChar > 0 && isWordChar(line, adjustedChar - 1)) {
			// Move to the last character of the word
			start = adjustedChar - 1
			end = adjustedChar - 1
		}
		// Check if we're at the start of a word
		else if (adjustedChar < line.length - 1 && isWordChar(line, adjustedChar + 1)) {
			// Move to the first character of the word
			start = adjustedChar + 1
			end = adjustedChar + 1
		} else {
			// We're not adjacent to any word
			return null
		}
	}

	// Find start of word by moving backwards
	while (start > 0 && isWordChar(line, start - 1)) {
		start--
	}

	// Find end of word by moving forwards
	while (end < line.length - 1 && isWordChar(line, end + 1)) {
		end++
	}

	// If we found a valid word range, create and return the WordSegment
	return if (end >= start) {
		WordSegment(
			text = line.substring(start, end + 1),
			range = TextEditorRange(
				start = CharLineOffset(position.line, start),
				end = CharLineOffset(position.line, end + 1)
			)
		)
	} else null
}

/**
 * Returns a list of word segments that fall within or intersect the specified text range.
 * A word segment represents a continuous sequence of word characters along with its position
 * in the text.
 *
 * The function will expand outward from the given range boundaries to ensure complete words
 * are captured even if the range starts or ends mid-word.
 *
 * @param range The text range to search for words within. Must be valid and within document bounds.
 * @return List of [WordSegment] objects, each containing the word text and its precise range
 *         in the document. Returns empty list if range is invalid or contains no words.
 */
fun TextEditorState.wordSegmentsInRange(range: TextEditorRange): List<WordSegment> {
	// Validate the range is within bounds
	if (range.start.line < 0 || range.end.line >= textLines.size) {
		return emptyList()
	}

	val segments = mutableListOf<WordSegment>()

	for (lineIndex in range.start.line..range.end.line) {
		val lineText = textLines[lineIndex].text
		if (lineText.isEmpty()) continue

		// Determine the character range within the line to check
		val startChar = if (lineIndex == range.start.line) range.start.char else 0
		val endChar = if (lineIndex == range.end.line) range.end.char else lineText.length

		// Expand outward from the range to find the first word start and end
		var wordStart = startChar
		while (wordStart > 0) {
			// Check if the current position is valid before calling isWordChar
			if (!isWordChar(lineText, wordStart)) {
				wordStart--
			} else {
				break
			}
		}

		var wordEnd = endChar
		while (wordEnd < lineText.length) {
			// Similarly check bounds here
			if (!isWordChar(lineText, wordEnd)) {
				wordEnd++
			} else {
				break
			}
		}

		// Process all words in the expanded range
		var i = wordStart
		while (i <= wordEnd && i < lineText.length) {
			// Check if the current character is part of a word
			if (isWordChar(lineText, i)) {
				// Find the full word boundaries
				var start = i
				while (start > 0 && isWordChar(lineText, start - 1)) {
					start--
				}

				var end = i
				while (end < lineText.length - 1 && isWordChar(lineText, end + 1)) {
					end++
				}

				// Add the word if it hasn't been added yet
				val wordSegment = WordSegment(
					text = lineText.substring(start, end + 1),
					range = TextEditorRange(
						start = CharLineOffset(lineIndex, start),
						end = CharLineOffset(lineIndex, end + 1)
					)
				)

				if (segments.none { it.range == wordSegment.range }) {
					segments.add(wordSegment)
				}

				// Move `i` past the current word
				i = end + 1
			} else {
				// Move to the next character
				i++
			}
		}
	}

	return segments
}
