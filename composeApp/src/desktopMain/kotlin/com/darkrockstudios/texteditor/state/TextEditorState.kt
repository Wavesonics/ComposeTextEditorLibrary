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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextRange
import com.darkrockstudios.texteditor.annotatedstring.splitToAnnotatedString
import com.darkrockstudios.texteditor.annotatedstring.subSequence
import com.darkrockstudios.texteditor.annotatedstring.toAnnotatedString
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.richstyle.RichSpanStyle
import kotlinx.coroutines.CoroutineScope
import kotlin.math.min

class TextEditorState(
	scope: CoroutineScope,
	internal val textMeasurer: TextMeasurer,
) {
	private var _version by mutableStateOf(0)
	internal val _textLines = mutableListOf<AnnotatedString>()
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
	private val editManager = TextEditManager(this)
	val richSpanManager = RichSpanManager(this)

	val scrollState get() = scrollManager.scrollState
	val totalContentHeight get() = scrollManager.totalContentHeight

	val editOperations = editManager.editOperations

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
		val operation = TextEditOperation.Insert(
			position = cursorPosition,
			text = AnnotatedString("\n"),
			cursorBefore = cursorPosition,
			cursorAfter = CharLineOffset(cursorPosition.line + 1, 0)
		)
		editManager.applyOperation(operation)
	}


	fun backspaceAtCursor() {
		if (cursorPosition.char > 0) {
			val deleteRange = TextRange(
				CharLineOffset(cursorPosition.line, cursorPosition.char - 1),
				cursorPosition
			)
			val deletedText = textLines[cursorPosition.line]
				.subSequence(deleteRange.start.char, deleteRange.end.char)

			val operation = TextEditOperation.Delete(
				range = deleteRange,
				deletedText = deletedText,
				cursorBefore = cursorPosition,
				cursorAfter = CharLineOffset(cursorPosition.line, cursorPosition.char - 1)
			)
			editManager.applyOperation(operation)
		} else if (cursorPosition.line > 0) {
			val previousLineLength = textLines[cursorPosition.line - 1].length
			val deleteRange = TextRange(
				CharLineOffset(cursorPosition.line - 1, previousLineLength),
				cursorPosition
			)
			val deletedText = AnnotatedString("\n")

			val operation = TextEditOperation.Delete(
				range = deleteRange,
				deletedText = deletedText,
				cursorBefore = cursorPosition,
				cursorAfter = CharLineOffset(cursorPosition.line - 1, previousLineLength)
			)
			editManager.applyOperation(operation)
		}
	}

	fun deleteAtCursor() {
		if (cursorPosition.char < textLines[cursorPosition.line].length) {
			val deleteRange = TextRange(
				cursorPosition,
				CharLineOffset(cursorPosition.line, cursorPosition.char + 1)
			)
			val deletedText = textLines[cursorPosition.line]
				.subSequence(deleteRange.start.char, deleteRange.end.char)

			val operation = TextEditOperation.Delete(
				range = deleteRange,
				deletedText = deletedText,
				cursorBefore = cursorPosition,
				cursorAfter = cursorPosition
			)
			editManager.applyOperation(operation)
		} else if (cursorPosition.line < textLines.size - 1) {
			val deleteRange = TextRange(
				cursorPosition,
				CharLineOffset(cursorPosition.line + 1, 0)
			)
			val deletedText = AnnotatedString("\n")

			val operation = TextEditOperation.Delete(
				range = deleteRange,
				deletedText = deletedText,
				cursorBefore = cursorPosition,
				cursorAfter = cursorPosition
			)
			editManager.applyOperation(operation)
		}
	}

	fun insertCharacterAtCursor(char: Char) {
		val operation = TextEditOperation.Insert(
			position = cursorPosition,
			text = AnnotatedString(char.toString()),
			cursorBefore = cursorPosition,
			cursorAfter = CharLineOffset(cursorPosition.line, cursorPosition.char + 1)
		)
		editManager.applyOperation(operation)
	}


	fun insertStringAtCursor(string: String) = insertStringAtCursor(string.toAnnotatedString())
	fun insertStringAtCursor(text: AnnotatedString) {
		// Convert to an Insert operation and let TextEditManager handle it
		val operation = TextEditOperation.Insert(
			position = cursorPosition,
			text = text,
			cursorBefore = cursorPosition,
			cursorAfter = CharLineOffset(cursorPosition.line, cursorPosition.char + text.length)
		)
		editManager.applyOperation(operation)
	}

	fun replace(range: TextRange, newText: String) = replace(range, newText.toAnnotatedString())
	fun replace(range: TextRange, newText: AnnotatedString) {
		// Create Replace operation and let TextEditManager handle it
		val operation = TextEditOperation.Replace(
			range = range,
			newText = newText,
			oldText = buildAnnotatedString {
				if (range.isSingleLine()) {
					// Single line - get text and spans from the range
					val line = textLines[range.start.line]
					append(line.subSequence(range.start.char, range.end.char))
				} else {
					// Multi-line - preserve text and spans across lines
					append(textLines[range.start.line].subSequence(range.start.char))
					append("\n")

					for (line in (range.start.line + 1) until range.end.line) {
						append(textLines[line])
						append("\n")
					}

					append(textLines[range.end.line].subSequence(0, range.end.char))
				}
			},
			cursorBefore = cursorPosition,
			cursorAfter = when {
				newText.contains('\n') -> {
					val lines = newText.split('\n')
					CharLineOffset(
						range.start.line + lines.size - 1,
						if (lines.size > 1) lines.last().length else range.start.char + newText.length
					)
				}

				else -> CharLineOffset(
					range.start.line,
					range.start.char + newText.length
				)
			}
		)

		editManager.applyOperation(operation)
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

	fun undo() {
		editManager.undo()
	}

	fun redo() {
		editManager.redo()
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

	fun CharLineOffset.toCharacterIndex(): Int {
		var totalChars = 0
		for (lineIndex in 0 until line) {
			totalChars += textLines[lineIndex].length + 1  // +1 for newline
		}
		return totalChars + char
	}

	// Convert character index to CharLineOffset
	fun Int.toCharLineOffset(): CharLineOffset {
		var remainingChars = this
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

	internal fun updateBookKeeping(affectedLines: IntRange? = null) {
		val offsets = mutableListOf<LineWrap>()
		var yOffset = 0f

		textLines.forEachIndexed { lineIndex, line ->
			val shouldRemeasure = affectedLines == null ||
					lineIndex in affectedLines ||
					lineIndex > (affectedLines.lastOrNull() ?: -1)

			val textLayoutResult = if (shouldRemeasure) {
				try {
					textMeasurer.measure(
						text = line,
						constraints = Constraints(
							maxWidth = maxOf(1, viewportSize.width.toInt()),
							minHeight = 0,
							maxHeight = Constraints.Infinity
						)
					)
				} catch (e: IllegalArgumentException) {
					println(e)
					// If measurement fails, create an empty layout result
					textMeasurer.measure(
						text = AnnotatedString(""),
						constraints = Constraints(
							maxWidth = maxOf(1, viewportSize.width.toInt()),
							minHeight = 0,
							maxHeight = Constraints.Infinity
						)
					)
				}
			} else {
				val existing = lineOffsets.find { it.line == lineIndex }?.textLayoutResult
				if (existing != null) {
					existing
				} else {
					textMeasurer.measure(
						text = line,
						constraints = Constraints(
							maxWidth = maxOf(1, viewportSize.width.toInt()),
							minHeight = 0,
							maxHeight = Constraints.Infinity
						)
					)
				}
			}

			val virtualLineCount = textLayoutResult.multiParagraph.lineCount

			for (virtualLineIndex in 0 until virtualLineCount) {
				val lineWrapsAt = if (virtualLineIndex == 0) {
					0
				} else {
					val prevEnd =
						textLayoutResult.getLineEnd(virtualLineIndex - 1, visibleEnd = true)
					(prevEnd + 1).coerceIn(0, line.length)
				}

				val lineLength =
					textLayoutResult.getLineEnd(virtualLineIndex) - textLayoutResult.getLineStart(
						virtualLineIndex
					)

				val lineWrap = LineWrap(
					line = lineIndex,
					wrapStartsAtIndex = lineWrapsAt,
					virtualLength = lineLength,
					virtualLineIndex = virtualLineIndex,
					offset = Offset(0f, yOffset),
					textLayoutResult = textLayoutResult
				)

				val richSpans = if (shouldRemeasure) {
					richSpanManager.getSpansForLineWrap(lineWrap)
				} else {
					lineOffsets.find {
						it.line == lineIndex && it.wrapStartsAtIndex == lineWrapsAt
					}?.richSpans ?: emptyList()
				}

				offsets.add(lineWrap.copy(richSpans = richSpans))
				yOffset += textLayoutResult.multiParagraph.getLineHeight(virtualLineIndex)
			}
		}

		lineOffsets = offsets
		scrollManager.updateContentHeight(yOffset.toInt())
	}

	fun addRichSpan(start: Int, end: Int, style: RichSpanStyle) {
		richSpanManager.addSpan(start.toCharLineOffset(), end.toCharLineOffset(), style)
		updateBookKeeping()
	}

	fun removeRichSpan(span: RichSpan) {
		richSpanManager.removeSpan(span)
		updateBookKeeping()
	}

	fun findSpanAtPosition(position: CharLineOffset): RichSpan? {
		// Find the line wrap that contains our position
		val lineWrap = lineOffsets.firstOrNull { wrap ->
			wrap.line == position.line && position.char >= wrap.wrapStartsAtIndex
		} ?: return null

		// Check each span in the line wrap
		return lineWrap.richSpans.firstOrNull { span ->
			span.containsPosition(position)
		}
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

enum class SpanClickType {
	TAP,
	PRIMARY_CLICK,
	SECONDARY_CLICK,
}