package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.CharLineOffset
import kotlin.math.min

internal fun TextEditorState.moveCursorLeft(n: Int = 1) {
	val currentCharIndex = getCharacterIndex(cursorPosition)
	val newCharIndex = maxOf(currentCharIndex - n, 0)
	updateCursorPosition(getOffsetAtCharacter(newCharIndex))
}

internal fun TextEditorState.moveCursorRight(n: Int = 1) {
	val currentCharIndex = getCharacterIndex(cursorPosition)
	val totalChars =
		textLines.sumOf { it.length + 1 } - 1  // -1 since last line doesn't have newline
	val newCharIndex = minOf(currentCharIndex + n, totalChars)
	updateCursorPosition(getOffsetAtCharacter(newCharIndex))
}

internal fun TextEditorState.moveCursorUp() {
	val currentWrappedIndex = getWrappedLineIndex(cursorPosition)
	if (currentWrappedIndex > 0) {
		val curWrappedSegment = lineOffsets[currentWrappedIndex]
		val previousWrappedSegment = lineOffsets[currentWrappedIndex - 1]
		val localCharIndex = cursorPosition.char - curWrappedSegment.wrapStartsAtIndex
		val newCharIndex = min(
			textLines[previousWrappedSegment.line].length,
			previousWrappedSegment.wrapStartsAtIndex + localCharIndex
		)

		updateCursorPosition(
			CharLineOffset(
				line = previousWrappedSegment.line,
				char = newCharIndex
			)
		)
	}
}

internal fun TextEditorState.moveCursorDown() {
	val currentWrappedIndex = getWrappedLineIndex(cursorPosition)
	if (currentWrappedIndex < lineOffsets.size - 1) {
		val curWrappedSegment = lineOffsets[currentWrappedIndex]
		val nextWrappedSegment = lineOffsets[currentWrappedIndex + 1]
		val localCharIndex = cursorPosition.char - curWrappedSegment.wrapStartsAtIndex

		val newCharIndex = min(
			textLines[nextWrappedSegment.line].length,
			nextWrappedSegment.wrapStartsAtIndex + localCharIndex
		)

		updateCursorPosition(
			CharLineOffset(
				line = nextWrappedSegment.line,
				char = newCharIndex
			)
		)
	}
}

internal fun TextEditorState.moveCursorToLineStart() {
	val currentWrappedLine = getWrappedLine(cursorPosition)
	updateCursorPosition(cursorPosition.copy(char = currentWrappedLine.wrapStartsAtIndex))
}

internal fun TextEditorState.moveCursorToLineEnd() {
	val (line, _) = cursorPosition
	val currentWrappedLineIndex = getWrappedLineIndex(cursorPosition)
	val currentWrappedLine = lineOffsets[currentWrappedLineIndex]

	if (currentWrappedLineIndex < lineOffsets.size - 1) {
		val nextWrappedLine = lineOffsets[currentWrappedLineIndex + 1]
		if (nextWrappedLine.line == currentWrappedLine.line) {
			// Go to the end of this virtual line
			updateCursorPosition(cursorPosition.copy(char = nextWrappedLine.wrapStartsAtIndex - 1))
		} else {
			// Go to the end of this real line
			updateCursorPosition(cursorPosition.copy(char = textLines[line].length))
		}
	} else {
		updateCursorPosition(cursorPosition.copy(char = textLines[line].length))
	}
}