package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.TextOffset
import kotlin.math.min

internal fun TextEditorState.moveCursorLeft() {
	val (line, charIndex) = cursorPosition
	if (charIndex > 0) {
		updateCursorPosition(cursorPosition.copy(char = charIndex - 1))
	} else if (line > 0) {
		updateCursorPosition(TextOffset(line - 1, textLines[line - 1].length))
	}
}

internal fun TextEditorState.moveCursorRight() {
	val (line, charIndex) = cursorPosition
	if (charIndex < textLines[line].length) {
		updateCursorPosition(cursorPosition.copy(char = charIndex + 1))
	} else if (line < textLines.size - 1) {
		updateCursorPosition(TextOffset(line + 1, 0))
	}
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
			TextOffset(
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
			TextOffset(
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