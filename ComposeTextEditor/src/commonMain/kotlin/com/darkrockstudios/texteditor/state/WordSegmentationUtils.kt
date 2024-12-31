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
	val char = text[pos]

	return when {
		// Basic word characters
		char.isLetterOrDigit() || char == '_' -> true

		// Special characters that might be part of a word
		char == '\'' -> {
			// Must have a letter before AND a letter after to be considered part of a word
			pos > 0 && pos < text.length - 1 &&
					text[pos - 1].let { prev -> prev.isLetter() || prev == '.' } &&
					text[pos + 1].isLetter()
		}

		char == '.' -> {
			// Handle periods in different contexts:
			// 1. Middle of abbreviation (U.S.A)
			// 2. End of abbreviation (U.S.A.)
			pos > 0 && text[pos - 1].isLetter() && (
					// Either has letter after (middle of abbreviation)
					(pos < text.length - 1 && text[pos + 1].isLetter()) ||
							// Or comes after another period-letter sequence (end of abbreviation)
							(pos > 1 && text[pos - 2] == '.')
					)
		}

		char == '-' -> {
			// Must have a letter before and after to be considered part of a word
			val hasPreviousLetter = pos > 0 && text[pos - 1].isLetter()
			val hasNextLetter = pos < text.length - 1 && text[pos + 1].isLetter()
			hasPreviousLetter && hasNextLetter
		}

		// Handle letters after apostrophes or periods
		char.isLetter() -> {
			pos > 0 && text[pos - 1].let { prev ->
				prev == '\'' || prev == '.'
			}
		}

		else -> false
	}
}