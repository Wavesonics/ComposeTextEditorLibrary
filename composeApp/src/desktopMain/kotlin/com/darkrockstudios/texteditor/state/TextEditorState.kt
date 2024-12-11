package com.darkrockstudios.texteditor.state

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextOffset

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

	val selector = TextEditorSelectionManager(this)

	fun updateContentHeight(height: Int) {
		totalContentHeight = height
	}

	internal fun notifyContentChanged() {
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

	internal fun replaceLine(index: Int, text: String) {
		_textLines[index] = text
	}

	internal fun removeLines(startIndex: Int, count: Int) {
		repeat(count) {
			_textLines.removeAt(startIndex)
		}
	}

	internal fun insertLine(index: Int, text: String) {
		_textLines.add(index, text)
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
}

@Composable
fun rememberTextEditorState(): TextEditorState {
	return remember { TextEditorState() }
}