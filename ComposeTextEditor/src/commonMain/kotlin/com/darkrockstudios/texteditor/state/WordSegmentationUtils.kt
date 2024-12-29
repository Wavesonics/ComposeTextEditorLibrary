package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange

fun TextEditorState.wordSegments(): Sequence<WordSegment> = sequence {
	textLines.asSequence().withIndex().forEach { (lineIndex, line) ->
		var wordStart = -1
		var currentChar = 0

		while (currentChar <= line.text.length) {
			val char = if (currentChar < line.text.length) line.text[currentChar] else ' '

			when {
				// Start of a new word
				wordStart == -1 && isWordChar(char) -> {
					wordStart = currentChar
				}

				// End of a word
				wordStart != -1 && !isWordChar(char) -> {
					yield(
						WordSegment(
							text = line.text.substring(wordStart, currentChar),
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

internal fun isWordChar(char: Char): Boolean {
	return char.isLetterOrDigit() || char == '_'
}