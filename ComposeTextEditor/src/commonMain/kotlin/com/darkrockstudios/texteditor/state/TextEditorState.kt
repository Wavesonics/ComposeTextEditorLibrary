package com.darkrockstudios.texteditor.state

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.annotatedstring.splitAnnotatedString
import com.darkrockstudios.texteditor.annotatedstring.subSequence
import com.darkrockstudios.texteditor.annotatedstring.toAnnotatedString
import com.darkrockstudios.texteditor.markdown.toMarkdown
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.richstyle.RichSpanStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.min
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TextEditorState(
	scope: CoroutineScope,
	measurer: TextMeasurer,
	initialText: AnnotatedString? = null
) {
	internal var textMeasurer: TextMeasurer = measurer
		set(value) {
			field = value
			updateBookKeeping()
		}

	private var _version by mutableStateOf(0)
	internal val _textLines = mutableListOf<AnnotatedString>()
	val textLines: List<AnnotatedString> get() = _textLines

	private val _cursorPositionFlow = MutableSharedFlow<CharLineOffset>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)
	val cursorPositionFlow: SharedFlow<CharLineOffset> = _cursorPositionFlow

	private var _cursorPosition by mutableStateOf(CharLineOffset(0, 0))
	var cursorPosition: CharLineOffset
		get() = _cursorPosition
		private set(value) {
			_cursorPosition = value
			_cursorPositionFlow.tryEmit(value)
		}

	var isCursorVisible by mutableStateOf(true)
	var isFocused by mutableStateOf(false)
	var lineOffsets by mutableStateOf(emptyList<LineWrap>())

	val selectionRangeFlow: SharedFlow<TextEditorRange?> get() = selector.selectionRangeFlow

	private var _canUndo by mutableStateOf(false)
	private var _canRedo by mutableStateOf(false)

	val canUndo: Boolean get() = _canUndo
	val canRedo: Boolean get() = _canRedo

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

	fun setText(text: String) {
		_textLines.clear()
		_textLines.addAll(text.split("\n").map { it.toAnnotatedString() })
		updateBookKeeping()
		notifyContentChanged()
	}

	fun setText(text: AnnotatedString) {
		_textLines.clear()
		_textLines.addAll(text.splitAnnotatedString())
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
		val maxLine = (_textLines.size - 1).coerceAtLeast(0)
		val line = position.line.coerceIn(0, maxLine)
		val char = position.char.coerceIn(0, _textLines.getOrNull(line)?.length ?: 0)

		cursorPosition = CharLineOffset(line, char)
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
			val deleteRange = TextEditorRange(
				CharLineOffset(cursorPosition.line, cursorPosition.char - 1),
				cursorPosition
			)

			val operation = TextEditOperation.Delete(
				range = deleteRange,
				cursorBefore = cursorPosition,
				cursorAfter = CharLineOffset(cursorPosition.line, cursorPosition.char - 1)
			)
			editManager.applyOperation(operation)
		} else if (cursorPosition.line > 0) {
			val previousLineLength = textLines[cursorPosition.line - 1].length
			val deleteRange = TextEditorRange(
				CharLineOffset(cursorPosition.line - 1, previousLineLength),
				cursorPosition
			)

			val operation = TextEditOperation.Delete(
				range = deleteRange,
				cursorBefore = cursorPosition,
				cursorAfter = CharLineOffset(cursorPosition.line - 1, previousLineLength)
			)
			editManager.applyOperation(operation)
		}
	}

	fun deleteAtCursor() {
		if (cursorPosition.char < textLines[cursorPosition.line].length) {
			val deleteRange = TextEditorRange(
				cursorPosition,
				CharLineOffset(cursorPosition.line, cursorPosition.char + 1)
			)

			val operation = TextEditOperation.Delete(
				range = deleteRange,
				cursorBefore = cursorPosition,
				cursorAfter = cursorPosition
			)
			editManager.applyOperation(operation)
		} else if (cursorPosition.line < textLines.size - 1) {
			val deleteRange = TextEditorRange(
				cursorPosition,
				CharLineOffset(cursorPosition.line + 1, 0)
			)

			val operation = TextEditOperation.Delete(
				range = deleteRange,
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
		val operation = TextEditOperation.Insert(
			position = cursorPosition,
			text = text,
			cursorBefore = cursorPosition,
			cursorAfter = CharLineOffset(cursorPosition.line, cursorPosition.char + text.length)
		)
		editManager.applyOperation(operation)
	}

	fun delete(range: TextEditorRange) {
		val operation = TextEditOperation.Delete(
			range = range,
			cursorBefore = cursorPosition,
			cursorAfter = range.start
		)
		editManager.applyOperation(operation)
	}

	fun replace(range: TextEditorRange, newText: String, inheritStyle: Boolean = false) =
		replace(range, newText.toAnnotatedString(), inheritStyle)

	fun replace(range: TextEditorRange, newText: AnnotatedString, inheritStyle: Boolean = false) {
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
			},
			inheritStyle = inheritStyle,
		)

		editManager.applyOperation(operation)
	}

	internal fun updateLine(index: Int, text: String) =
		updateLine(index, text.toAnnotatedString())

	internal fun updateLine(index: Int, text: AnnotatedString) {
		_textLines[index] = text
		updateBookKeeping()
	}

	internal fun removeLines(startIndex: Int, count: Int) {
		// If there are no lines, or we're trying to remove more lines than exist, abort
		if (_textLines.isEmpty() || startIndex >= _textLines.size) {
			return
		}

		// Ensure we don't remove more lines than available
		val safeCount = minOf(count, _textLines.size - startIndex)

		// Always keep at least one empty line
		if (_textLines.size <= safeCount) {
			_textLines.clear()
			_textLines.add(AnnotatedString(""))
		} else {
			repeat(safeCount) {
				_textLines.removeAt(startIndex)
			}
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
			//val textLayoutResult = lineWrap.textLayoutResult
			val textLayoutResult = textMeasurer.measure(
				textLines[lineWrap.line],
				constraints = Constraints(maxWidth = viewportSize.width.toInt())
			)

			val relativeOffset = offset - lineWrap.offset
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

	fun wrapStartToCharacterIndex(lineWrap: LineWrap): Int {
		// First get the physical line start offset
		val physicalLineStartOffset = getLineStartOffset(lineWrap.line)
		// Add the local offset from the LineWrap
		return physicalLineStartOffset + lineWrap.wrapStartsAtIndex
	}

	fun getLineStartOffset(lineIndex: Int): Int {
		require(lineIndex >= 0) { "Line index must be non-negative" }
		require(lineIndex < textLines.size) { "Line index $lineIndex out of bounds for ${textLines.size} lines" }

		var offset = 0
		// Sum up lengths of all previous lines
		for (i in 0 until lineIndex) {
			offset += textLines[i].length
			// Add 1 for the newline character at the end of each line
			// except for the last line if it doesn't end with a newline
			offset += 1
		}
		return offset
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

		_canUndo = editManager.history.hasUndoLevels()
		_canRedo = editManager.history.hasRedoLevels()
	}

	fun addStyleSpan(range: TextEditorRange, style: SpanStyle) {
		editManager.addSpanStyle(range, style)
		updateBookKeeping()
	}

	fun removeStyleSpan(range: TextEditorRange, style: SpanStyle) {
		editManager.removeStyleSpan(range, style)
		updateBookKeeping()
	}

	fun addRichSpan(range: TextEditorRange, style: RichSpanStyle) {
		richSpanManager.addRichSpan(range, style)
		updateBookKeeping()
	}

	fun addRichSpan(start: CharLineOffset, end: CharLineOffset, style: RichSpanStyle) {
		richSpanManager.addRichSpan(start, end, style)
		updateBookKeeping()
	}

	fun addRichSpan(start: Int, end: Int, style: RichSpanStyle) {
		richSpanManager.addRichSpan(start.toCharLineOffset(), end.toCharLineOffset(), style)
		updateBookKeeping()
	}

	fun removeRichSpan(start: CharLineOffset, end: CharLineOffset, style: RichSpanStyle) {
		richSpanManager.removeRichSpan(start, end, style)
		updateBookKeeping()
	}

	fun removeRichSpan(span: RichSpan) {
		richSpanManager.removeRichSpan(span)
		updateBookKeeping()
	}

	fun findSpanAtPosition(position: CharLineOffset): RichSpan? {
		// Find the line wrap that contains our position
		val lineWrap = lineOffsets.lastOrNull { wrap ->
			wrap.line == position.line && position.char >= wrap.wrapStartsAtIndex
		} ?: return null

		// Check each span in the line wrap
		return lineWrap.richSpans.firstOrNull { span ->
			span.containsPosition(position)
		}
	}

	fun captureMetadata(range: TextEditorRange): OperationMetadata {
		val deletedContent = when {
			range.isSingleLine() -> {
				textLines[range.start.line].subSequence(range.start.char, range.end.char)
			}

			else -> {
				buildAnnotatedString {
					// First line - from start to end
					append(textLines[range.start.line].subSequence(range.start.char))
					append("\n")

					// Middle lines
					for (line in (range.start.line + 1) until range.end.line) {
						append(textLines[line])
						append("\n")
					}

					// Last line - up to end char
					if (range.end.line < textLines.size) {
						append(textLines[range.end.line].subSequence(0, range.end.char))
					}
				}
			}
		}

		return OperationMetadata(
			deletedText = deletedContent,
			deletedSpans = richSpanManager.getSpansInRange(range),
			preservedRichSpans = richSpanManager.getSpansInRange(range).map { span ->
				PreservedRichSpan(
					relativeStart = getRelativePosition(span.range.start, range.start),
					relativeEnd = getRelativePosition(span.range.end, range.start),
					style = span.style
				)
			}
		)
	}

	private fun getRelativePosition(
		pos: CharLineOffset,
		basePos: CharLineOffset
	): RelativePosition {
		val lineDiff = pos.line - basePos.line
		val char = when {
			lineDiff == 0 -> pos.char - basePos.char
			lineDiff > 0 -> pos.char  // On later line, keep char position
			else -> pos.char          // Should not happen in properly bounded spans
		}
		return RelativePosition(lineDiff, char)
	}

	internal fun getLine(lineIndex: Int): AnnotatedString = textLines[lineIndex]

	fun getStringInRange(range: TextEditorRange): String {
		return if (range.isSingleLine()) {
			textLines[range.start.line].text.substring(range.start.char, range.end.char)
		} else {
			buildString {
				// First line
				append(textLines[range.start.line].text.substring(range.start.char))
				append('\n')

				// Middle lines
				for (line in (range.start.line + 1) until range.end.line) {
					append(textLines[line].text)
					append('\n')
				}

				// Last line
				append(textLines[range.end.line].text.substring(0, range.end.char))
			}
		}
	}

	fun getTextInRange(range: TextEditorRange): AnnotatedString {
		return if (range.isSingleLine()) {
			// For single line, we can use subSequence which preserves spans
			textLines[range.start.line].subSequence(range.start.char, range.end.char)
		} else {
			buildAnnotatedString {
				// First line - from start to end, preserving spans
				append(textLines[range.start.line].subSequence(range.start.char))
				append('\n')

				// Middle lines - complete lines with their spans
				for (line in (range.start.line + 1) until range.end.line) {
					append(textLines[line])
					append('\n')
				}

				// Last line - up to end char, preserving spans
				if (range.end.line < textLines.size) {
					append(textLines[range.end.line].subSequence(0, range.end.char))
				}
			}
		}
	}

	fun getAllText(): AnnotatedString {
		return buildAnnotatedString {
			textLines.forEach { line ->
				append(line)
				append('\n')
			}
		}
	}

	fun exportAsMarkdown(): String {
		return getAllText().toMarkdown()
	}

	fun computeTextHash(): Int {
		var hash = 3
		val multiplier = 31
		textLines.forEach { line ->
			hash = multiplier * hash + line.hashCode()
		}
		return hash
	}

	init {
		if (initialText != null) {
			setText(initialText)
		}
	}
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun rememberTextEditorState(initialText: AnnotatedString? = null): TextEditorState {
	val scope = rememberCoroutineScope()
	val density = LocalDensity.current
	val windowInfo = LocalWindowInfo.current

	// Trigger recomposition when window info changes
	val measuringKey = remember(density, windowInfo) { Uuid.random() }
	val textMeasurer = rememberTextMeasurer()

	val state = remember {
		TextEditorState(
			scope = scope,
			measurer = textMeasurer,
			initialText = initialText,
		)
	}

	LaunchedEffect(measuringKey, textMeasurer) {
		state.textMeasurer = textMeasurer
	}

	return state
}

enum class SpanClickType {
	TAP,
	PRIMARY_CLICK,
	SECONDARY_CLICK,
}