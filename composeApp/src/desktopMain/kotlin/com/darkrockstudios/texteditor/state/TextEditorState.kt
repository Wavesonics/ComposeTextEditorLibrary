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

	internal fun notifyContentChanged(operation: TextEditOperation? = null) {
		operation?.let { richSpanManager.updateSpans(it) }
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
		val (line, charIndex) = cursorPosition
		val currentLine = textLines[line]

		// Merge the new text with existing styles
		val newLine = mergeSpanStylesForInsertion(currentLine, charIndex, text)

		// Update the line
		_textLines[line] = newLine
		updateBookKeeping()
		notifyContentChanged()
		updateCursorPosition(CharLineOffset(line, charIndex + text.length))
	}

	fun replace(range: TextRange, newText: String) = replace(range, newText.toAnnotatedString())
	fun replace(range: TextRange, newText: AnnotatedString) {
		val oldText = buildAnnotatedString {
			if (range.start.line == range.end.line) {
				// Single line replacement - preserve spans from the replaced section
				val line = textLines[range.start.line]
				val replacedSection = line.subSequence(range.start.char, range.end.char)
				append(replacedSection)

				// Copy all span styles from the replaced section
				replacedSection.spanStyles.forEach { span ->
					addStyle(span.item, span.start - range.start.char, span.end - range.start.char)
				}
			} else {
				// Multi-line replacement
				// First line - preserve spans
				val firstLine = textLines[range.start.line]
				val firstSection = firstLine.subSequence(range.start.char)
				append(firstSection)
				firstSection.spanStyles.forEach { span ->
					addStyle(span.item, span.start - range.start.char, span.end - range.start.char)
				}
				append("\n")

				// Middle lines - preserve spans
				for (line in (range.start.line + 1) until range.end.line) {
					val fullLine = textLines[line]
					append(fullLine)
					// Adjust span positions for the accumulated text
					val lineStartOffset = length - fullLine.length
					fullLine.spanStyles.forEach { span ->
						addStyle(
							span.item,
							span.start + lineStartOffset,
							span.end + lineStartOffset
						)
					}
					append("\n")
				}

				// Last line - preserve spans
				val lastLine = textLines[range.end.line]
				val lastSection = lastLine.subSequence(0, range.end.char)
				val lastLineOffset = length
				append(lastSection)
				lastSection.spanStyles.forEach { span ->
					addStyle(span.item, span.start + lastLineOffset, span.end + lastLineOffset)
				}
			}
		}

		val operation = TextEditOperation.Replace(
			range = range,
			newText = newText,
			oldText = oldText,
			cursorBefore = cursorPosition,
			cursorAfter = when {
				newText.contains('\n') -> {
					val lines = newText.split('\n')
					CharLineOffset(
						range.start.line + lines.size - 1,
						lines.last().length
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

	private fun mergeSpanStylesForInsertion(
		original: AnnotatedString,
		insertionIndex: Int,
		newText: AnnotatedString
	): AnnotatedString = buildAnnotatedString {
		// First, append all text
		append(original.subSequence(0, insertionIndex))
		append(newText)
		append(original.subSequence(insertionIndex))

		// Get spans that end at our insertion point
		val spansEndingAtInsertion = original.spanStyles.filter {
			it.start < insertionIndex && it.end == insertionIndex
		}

		// Get spans that start at our insertion point
		val spansStartingAtInsertion = original.spanStyles.filter {
			it.start == insertionIndex
		}

		// Handle existing spans before insertion point
		original.spanStyles.forEach { span ->
			when {
				// Span ends before insertion - keep as is
				span.end <= insertionIndex -> {
					addStyle(span.item, span.start, span.end)
				}

				// Span starts after insertion - shift by inserted length
				span.start >= insertionIndex -> {
					addStyle(
						span.item,
						span.start + newText.length,
						span.end + newText.length
					)
				}

				// Span crosses insertion point - extend to cover new text
				span.start < insertionIndex && span.end > insertionIndex -> {
					addStyle(
						span.item,
						span.start,
						span.end + newText.length
					)
				}
			}
		}

		// If we have spans ending at insertion and starting at insertion with the same style,
		// merge them to include the new text
		spansEndingAtInsertion.forEach { endingSpan ->
			spansStartingAtInsertion.forEach { startingSpan ->
				if (endingSpan.item == startingSpan.item) {
					addStyle(
						endingSpan.item,
						endingSpan.start,
						startingSpan.end + newText.length
					)
				}
			}
		}

		// Add any spans from the new text, shifted by insertion position
		newText.spanStyles.forEach { span ->
			addStyle(
				span.item,
				span.start + insertionIndex,
				span.end + insertionIndex
			)
		}
	}

	internal fun updateBookKeeping() {
		val offsets = mutableListOf<LineWrap>()
		var yOffset = 0f

		textLines.forEachIndexed { lineIndex, line ->
			val textLayoutResult = textMeasurer.measure(
				line,
				constraints = Constraints(
					maxWidth = maxOf(1, viewportSize.width.toInt()),
					minHeight = 0,
					maxHeight = Constraints.Infinity
				)
			)

			for (virtualLineIndex in 0 until textLayoutResult.multiParagraph.lineCount) {
				val lineWrapsAt = if (virtualLineIndex == 0) {
					0
				} else {
					textLayoutResult.getLineEnd(virtualLineIndex - 1, visibleEnd = true) + 1
				}

				// Calculate rich spans for this line wrap
				val lineWrap = LineWrap(
					line = lineIndex,
					wrapStartsAtIndex = lineWrapsAt,
					offset = Offset(0f, yOffset),
					textLayoutResult = textLayoutResult
				)

				val enhancedWrap = LineWrap(
					line = lineIndex,
					wrapStartsAtIndex = lineWrapsAt,
					offset = Offset(0f, yOffset),
					textLayoutResult = textLayoutResult,
					richSpans = richSpanManager.getSpansForLineWrap(lineWrap)
				)

				offsets.add(enhancedWrap)

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