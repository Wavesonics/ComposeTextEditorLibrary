package com.darkrockstudios.texteditor.state

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextRange
import com.darkrockstudios.texteditor.annotatedstring.splitToAnnotatedString
import com.darkrockstudios.texteditor.annotatedstring.subSequence
import com.darkrockstudios.texteditor.annotatedstring.toAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlin.math.min

class TextEditorState(
	scope: CoroutineScope,
	internal val textMeasurer: TextMeasurer,
) {
	private var _version by mutableStateOf(0)
	private val _textLines = mutableListOf<AnnotatedString>()
	val textLines: List<AnnotatedString> get() = _textLines

	var cursorPosition by mutableStateOf(CharLineOffset(0, 0))
	var isCursorVisible by mutableStateOf(true)
	var isFocused by mutableStateOf(false)
	var lineOffsets by mutableStateOf(emptyList<LineWrap>())

	private var viewportSize: Size = Size(1f, 1f)

	val scrollManager = TextEditorScrollManager(
		scope = scope,
		scrollState = ScrollState(0),
		getLines = { textLines },
		getViewportSize = { viewportSize },
		getCursorPosition = { cursorPosition },
		getLineOffsets = { lineOffsets },
	)

	val selector = TextEditorSelectionManager(this)

	val scrollState get() = scrollManager.scrollState
	val totalContentHeight get() = scrollManager.totalContentHeight

	internal fun notifyContentChanged() {
		_version++
	}

	fun setInitialText(text: String) {
		_textLines.clear()
		_textLines.addAll(text.split("\n").map { it.toAnnotatedString() })
		updateBookKeeping()
		notifyContentChanged()
	}

	fun setInitialText(text: AnnotatedString) {
		_textLines.clear()
		_textLines.addAll(text.splitToAnnotatedString())
		updateBookKeeping()
		notifyContentChanged()
	}

	fun setCursorVisible() {
		isCursorVisible = true
	}

	fun toggleCursor() {
		isCursorVisible = !isCursorVisible
	}

	fun updateFocus(focused: Boolean) {
		isFocused = focused
	}

	fun updateCursorPosition(position: CharLineOffset) {
		cursorPosition = position
		scrollManager.ensureCursorVisible()
	}

	fun insertNewlineAtCursor() {
		val (line, charIndex) = cursorPosition
		val newLine = _textLines[line].subSequence(charIndex)
		_textLines[line] = _textLines[line].subSequence(0, charIndex)
		_textLines.add(line + 1, newLine)
		updateBookKeeping()
		notifyContentChanged()
		updateCursorPosition(CharLineOffset(line + 1, 0))
	}

	fun backspaceAtCursor() {
		val (line, charIndex) = cursorPosition
		if (charIndex > 0) {
			_textLines[line] =
				_textLines[line].subSequence(0, charIndex - 1) + _textLines[line].subSequence(
					charIndex
				)
			updateBookKeeping()
			notifyContentChanged()
			updateCursorPosition(CharLineOffset(line, charIndex - 1))
		} else if (line > 0) {
			val previousLineLength = _textLines[line - 1].length
			_textLines[line - 1] += _textLines[line]
			_textLines.removeAt(line)
			updateBookKeeping()
			notifyContentChanged()
			updateCursorPosition(CharLineOffset(line - 1, previousLineLength))
		}
	}

	fun deleteAtCursor() {
		val (line, charIndex) = cursorPosition
		if (charIndex < _textLines[line].length) {
			_textLines[line] =
				_textLines[line].subSequence(
					0,
					charIndex
				) + _textLines[line].subSequence(charIndex + 1)
			updateBookKeeping()
			notifyContentChanged()
		} else if (line < _textLines.size - 1) {
			_textLines[line] += _textLines[line + 1]
			_textLines.removeAt(line + 1)
			updateBookKeeping()
			notifyContentChanged()
		}
	}

	fun insertCharacterAtCursor(char: Char) {
		val (line, charIndex) = cursorPosition
		_textLines[line] =
			_textLines[line].subSequence(
				0,
				charIndex
			) + AnnotatedString("$char") + _textLines[line].subSequence(
				startIndex = charIndex
			)
		updateBookKeeping()
		notifyContentChanged()
		updateCursorPosition(CharLineOffset(line, charIndex + 1))
	}

	fun insertStringAtCursor(string: String) = insertStringAtCursor(string.toAnnotatedString())
	fun insertStringAtCursor(string: AnnotatedString) {
		val (line, charIndex) = cursorPosition
		val thisLine = _textLines[line]
		_textLines[line] =
			thisLine.subSequence(
				0,
				charIndex
			) + string + thisLine.subSequence(startIndex = charIndex, endIndex = thisLine.length)
		updateBookKeeping()
		notifyContentChanged()
		updateCursorPosition(CharLineOffset(line, charIndex + 1))
	}

	fun replace(range: TextRange, newText: String) = replace(range, newText.toAnnotatedString())
	fun replace(range: TextRange, newText: AnnotatedString) {
		val newLines = newText.split('\n')

		when {
			// Single line replacement
			range.isSingleLine() -> {
				val line = _textLines[range.start.line]
				_textLines[range.start.line] = line.subSequence(0, range.start.char) +
						newText +
						line.subSequence(range.end.char)
				updateBookKeeping()
				notifyContentChanged()

				// Calculate new cursor position
				when {
					newLines.size > 1 -> {
						// Multi-line replacement text
						val lastLine = range.start.line + newLines.size - 1
						updateCursorPosition(CharLineOffset(lastLine, newLines.last().length))
					}

					else -> {
						// Single line replacement text
						updateCursorPosition(
							CharLineOffset(
								range.start.line,
								range.start.char + newText.length
							)
						)
					}
				}
			}

			// Multi-line replacement
			else -> {
				// Handle first line
				val firstLine = _textLines[range.start.line]
				val startText = firstLine.substring(0, range.start.char)

				// Handle last line
				val lastLine = _textLines[range.end.line]
				val endText = lastLine.substring(range.end.char)

				// Remove all lines in the range
				removeLines(range.start.line, range.end.line - range.start.line + 1)

				when (newLines.size) {
					0 -> {
						// Empty replacement
						insertLine(range.start.line, startText + endText)
						updateCursorPosition(CharLineOffset(range.start.line, startText.length))
					}

					1 -> {
						// Single line replacement
						insertLine(range.start.line, startText + newLines[0] + endText)
						updateCursorPosition(
							CharLineOffset(
								range.start.line,
								startText.length + newLines[0].length
							)
						)
					}

					else -> {
						// Multi-line replacement
						// First line
						insertLine(range.start.line, startText + newLines[0])

						// Middle lines
						for (i in 1 until newLines.size - 1) {
							insertLine(range.start.line + i, newLines[i])
						}

						// Last line
						val lastIndex = range.start.line + newLines.size - 1
						insertLine(lastIndex, newLines.last() + endText)

						updateCursorPosition(CharLineOffset(lastIndex, newLines.last().length))
					}
				}

				updateBookKeeping()
				notifyContentChanged()
			}
		}
	}

	internal fun replaceLine(index: Int, text: String) =
		replaceLine(index, text.toAnnotatedString())

	internal fun replaceLine(index: Int, text: AnnotatedString) {
		_textLines[index] = text
		updateBookKeeping()
	}

	internal fun removeLines(startIndex: Int, count: Int) {
		repeat(count) {
			_textLines.removeAt(startIndex)
		}
		updateBookKeeping()
	}

	internal fun insertLine(index: Int, text: String) = insertLine(index, text.toAnnotatedString())
	internal fun insertLine(index: Int, text: AnnotatedString) {
		_textLines.add(index, text)
		updateBookKeeping()
	}

	// Helper functions for cursor movement
	fun getWrappedLineIndex(position: CharLineOffset): Int {
		return lineOffsets.indexOfLast { lineOffset ->
			lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
		}
	}

	fun getWrappedLine(position: CharLineOffset): LineWrap {
		return lineOffsets.last { lineOffset ->
			lineOffset.line == position.line && lineOffset.wrapStartsAtIndex <= position.char
		}
	}

	fun onViewportSizeChange(size: Size) {
		viewportSize = size
		updateBookKeeping()
	}

	fun getOffsetAtPosition(offset: Offset): CharLineOffset {
		if (lineOffsets.isEmpty()) return CharLineOffset(0, 0)

		var curRealLine: LineWrap = lineOffsets[0]

		// Find the line that contains the offset
		for (lineWrap in lineOffsets) {
			if (lineWrap.line != curRealLine.line) {
				curRealLine = lineWrap
			}

			val textLayoutResult = textMeasurer.measure(
				textLines[lineWrap.line],
				constraints = Constraints(maxWidth = viewportSize.width.toInt())
			)

			val relativeOffset = offset - curRealLine.offset
			if (offset.y in curRealLine.offset.y..(curRealLine.offset.y + textLayoutResult.size.height)) {
				val charPos = textLayoutResult.multiParagraph.getOffsetForPosition(relativeOffset)
				return CharLineOffset(lineWrap.line, min(charPos, textLines[lineWrap.line].length))
			}
		}

		// If we're below all lines, return position at end of last line
		val lastLine = textLines.lastIndex
		return CharLineOffset(lastLine, textLines[lastLine].length)
	}

	fun getOffsetAtCharacter(index: Int): CharLineOffset {
		var remainingChars = index

		for (lineIndex in textLines.indices) {
			val lineLength = textLines[lineIndex].length + 1  // +1 for newline
			if (remainingChars < lineLength) {
				return CharLineOffset(lineIndex, remainingChars)
			}
			remainingChars -= lineLength
		}

		return CharLineOffset(
			textLines.lastIndex,
			textLines.last().length
		)
	}

	fun getCharacterIndex(offset: CharLineOffset): Int {
		var totalChars = 0

		// Add up characters from previous lines
		for (lineIndex in 0 until offset.line) {
			totalChars += textLines[lineIndex].length + 1  // +1 for newline
		}

		// Add characters in current line
		totalChars += offset.char

		return totalChars
	}


	private fun updateBookKeeping() {
		val offsets = mutableListOf<LineWrap>()
		var yOffset = 0f  // Track absolute Y position from top of entire content

		textLines.forEachIndexed { lineIndex, line ->
			val textLayoutResult = textMeasurer.measure(
				line,
				constraints = Constraints(
					maxWidth = maxOf(1, viewportSize.width.toInt()),
					minHeight = 0,
					maxHeight = Constraints.Infinity
				)
			)

			// Process each virtual line (word wrap creates multiple virtual lines)
			for (virtualLineIndex in 0 until textLayoutResult.multiParagraph.lineCount) {
				val lineWrapsAt = if (virtualLineIndex == 0) {
					0
				} else {
					textLayoutResult.getLineEnd(virtualLineIndex - 1, visibleEnd = true) + 1
				}

				offsets.add(
					LineWrap(
						line = lineIndex,
						wrapStartsAtIndex = lineWrapsAt,
						offset = Offset(0f, yOffset),
					)
				)

				yOffset += textLayoutResult.multiParagraph.getLineHeight(virtualLineIndex)
			}
		}

		lineOffsets = offsets
		scrollManager.updateContentHeight(yOffset.toInt())
	}
}

@Composable
fun rememberTextEditorState(): TextEditorState {
	val scope = rememberCoroutineScope()
	val textMeasurer = rememberTextMeasurer()

	return remember {
		TextEditorState(
			scope = scope,
			textMeasurer = textMeasurer,
		)
	}
}