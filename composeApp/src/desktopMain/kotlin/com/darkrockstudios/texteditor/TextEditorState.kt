package com.darkrockstudios.texteditor

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.min

class TextEditorState {
	private var _version by mutableStateOf(0)
	private val _textLines = mutableListOf<String>("")
	val textLines: List<String> get() = _textLines

	var cursorPosition by mutableStateOf(TextOffset(0, 0))
	var isCursorVisible by mutableStateOf(true)
	var isFocused by mutableStateOf(false)
	var lineOffsets by mutableStateOf(emptyList<LineWrap>())

	val scrollState = ScrollState(0)
	var totalContentHeight by mutableStateOf(0)
		private set

	fun updateContentHeight(height: Int) {
		totalContentHeight = height
	}

	private fun notifyContentChanged() {
		_version++
	}

	fun setInitialText(text: String) {
		_textLines.clear()
		_textLines.addAll(text.split("\n"))
		notifyContentChanged()
	}

	fun updateLineOffsets(newOffsets: List<LineWrap>) {
		lineOffsets = newOffsets
	}

	fun toggleCursor() {
		isCursorVisible = !isCursorVisible
	}

	fun updateFocus(focused: Boolean) {
		isFocused = focused
	}

	fun updateCursorPosition(position: TextOffset) {
		cursorPosition = position
	}

	fun insertNewlineAtCursor() {
		val (line, charIndex) = cursorPosition
		val newLine = _textLines[line].substring(charIndex)
		_textLines[line] = _textLines[line].substring(0, charIndex)
		_textLines.add(line + 1, newLine)
		notifyContentChanged()
		updateCursorPosition(TextOffset(line + 1, 0))
	}

	fun backspaceAtCursor() {
		val (line, charIndex) = cursorPosition
		if (charIndex > 0) {
			_textLines[line] =
				_textLines[line].substring(0, charIndex - 1) + _textLines[line].substring(charIndex)
			notifyContentChanged()
			updateCursorPosition(TextOffset(line, charIndex - 1))
		} else if (line > 0) {
			val previousLineLength = _textLines[line - 1].length
			_textLines[line - 1] += _textLines[line]
			_textLines.removeAt(line)
			notifyContentChanged()
			updateCursorPosition(TextOffset(line - 1, previousLineLength))
		}
	}

	fun deleteAtCursor() {
		val (line, charIndex) = cursorPosition
		if (charIndex < _textLines[line].length) {
			_textLines[line] =
				_textLines[line].substring(0, charIndex) + _textLines[line].substring(charIndex + 1)
			notifyContentChanged()
		} else if (line < _textLines.size - 1) {
			_textLines[line] += _textLines[line + 1]
			_textLines.removeAt(line + 1)
			notifyContentChanged()
		}
	}

	fun insertCharacterAtCursor(char: Char) {
		val (line, charIndex) = cursorPosition
		_textLines[line] =
			_textLines[line].substring(0, charIndex) + char + _textLines[line].substring(charIndex)
		notifyContentChanged()
		updateCursorPosition(TextOffset(line, charIndex + 1))
	}

	// Helper functions for cursor movement
	fun getWrappedLineIndex(position: TextOffset): Int {
		return lineOffsets.indexOfLast { lineOffset ->
			lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
		}
	}

	fun getWrappedLine(position: TextOffset): LineWrap {
		return lineOffsets.last { lineOffset ->
			lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
		}
	}

	// Cursor movement functions
	fun moveCursorLeft() {
		val (line, charIndex) = cursorPosition
		if (charIndex > 0) {
			updateCursorPosition(cursorPosition.copy(char = charIndex - 1))
		} else if (line > 0) {
			updateCursorPosition(TextOffset(line - 1, textLines[line - 1].length))
		}
	}

	fun moveCursorRight() {
		val (line, charIndex) = cursorPosition
		if (charIndex < textLines[line].length) {
			updateCursorPosition(cursorPosition.copy(char = charIndex + 1))
		} else if (line < textLines.size - 1) {
			updateCursorPosition(TextOffset(line + 1, 0))
		}
	}

	fun moveCursorUp() {
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

	fun moveCursorDown() {
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

	fun moveCursorToLineStart() {
		val currentWrappedLine = getWrappedLine(cursorPosition)
		updateCursorPosition(cursorPosition.copy(char = currentWrappedLine.wrapStartsAtIndex))
	}

	fun moveCursorToLineEnd() {
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
}


@Composable
fun rememberTextEditorState(): TextEditorState {
	return remember { TextEditorState() }
}