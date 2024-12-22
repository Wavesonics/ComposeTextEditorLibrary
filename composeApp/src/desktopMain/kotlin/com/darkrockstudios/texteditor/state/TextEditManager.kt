package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextRange
import com.darkrockstudios.texteditor.annotatedstring.toAnnotatedString
import com.darkrockstudios.texteditor.toCharacterIndex
import com.darkrockstudios.texteditor.wrapStartToCharacterIndex
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class TextEditManager(private val state: TextEditorState) {
	private val spanManager = SpanManager()
	private val history = TextEditHistory()

	private val _editOperations = MutableSharedFlow<TextEditOperation>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)
	val editOperations: SharedFlow<TextEditOperation> = _editOperations

	fun applyOperation(operation: TextEditOperation, addToHistory: Boolean = true) {
		val metadata = when (operation) {
			is TextEditOperation.Insert -> applyInsert(operation)
			is TextEditOperation.Delete -> applyDelete(addToHistory, operation)
			is TextEditOperation.Replace -> applyReplace(addToHistory, operation)
		}

		state.updateCursorPosition(operation.cursorAfter)
		state.richSpanManager.updateSpans(operation, metadata)
		state.updateBookKeeping()
		if (addToHistory) {
			history.recordEdit(operation, metadata ?: OperationMetadata())
		}
		state.notifyContentChanged()

		_editOperations.tryEmit(operation)
	}

	private fun applyInsert(operation: TextEditOperation.Insert): OperationMetadata? {
		if (operation.text.contains('\n')) {
			handleMultiLineInsert(operation)
		} else {
			// Single line insert with span merging
			val line = state._textLines[operation.position.line]
			state._textLines[operation.position.line] = mergeSpanStyles(
				line,
				operation.position.char,
				operation.text
			)
		}
		return null
	}

	private fun handleDelete(
		line: AnnotatedString,
		start: Int,
		end: Int
	): AnnotatedString = buildAnnotatedString {
		// Append text without deleted portion
		append(line.text.substring(0, start))
		append(line.text.substring(end))

		// We need to properly handle the spans
		line.spanStyles.forEach { span ->
			when {
				// Span ends before deletion - keep as is
				span.end <= start -> {
					addStyle(span.item, span.start, span.end)
				}
				// Span starts after deletion - adjust position
				span.start >= end -> {
					val newStart = span.start - (end - start)
					val newEnd = span.end - (end - start)
					addStyle(span.item, newStart, newEnd)
				}
				// Span overlaps deletion start - truncate at deletion
				span.start < start && span.end > start -> {
					addStyle(span.item, span.start, start)
				}
				// Span overlaps deletion end - adjust start
				span.start < end && span.end > end -> {
					val newStart = start
					val newEnd = span.end - (end - start)
					addStyle(span.item, newStart, newEnd)
				}
			}
		}
	}

	private fun handleReplace(
		line: AnnotatedString,
		start: Int,
		end: Int,
		newText: AnnotatedString
	): AnnotatedString = buildAnnotatedString {
		// Safely handle text portions with bounds checking
		val safeStart = start.coerceIn(0, line.length)
		val safeEnd = end.coerceIn(0, line.length)

		// Append text portions
		append(line.text.substring(0, safeStart))
		append(newText)
		if (safeEnd < line.length) {
			append(line.text.substring(safeEnd))
		}

		// Process spans using SpanManager
		val spanManager = SpanManager()
		val processedSpans = spanManager.processSpans(
			originalText = line,
			deletionStart = safeStart,
			deletionEnd = safeEnd,
			insertionPoint = safeStart,
			insertedText = newText
		)

		// Add processed spans to the result
		processedSpans.forEach { span ->
			addStyle(span.item, span.start, span.end)
		}
	}

	private fun handleMultiLineInsert(operation: TextEditOperation.Insert) {
		val insertLines = operation.text.text.split('\n')
		val currentLine = state._textLines[operation.position.line]
		val currentLineText = currentLine.text

		// Split current line content
		val prefixEndIndex = operation.position.char.coerceIn(0, currentLineText.length)
		val prefix = currentLineText.substring(0, prefixEndIndex)
		val suffix = currentLineText.substring(prefixEndIndex)

		// Create first line combining prefix with first inserted line
		val firstLineText = buildAnnotatedString {
			append(prefix)
			append(insertLines.first())

			// Add spans that belong to the prefix
			currentLine.spanStyles.forEach { span ->
				if (span.start < prefixEndIndex) {
					// If span ends beyond prefix, truncate it
					val spanEnd = minOf(span.end, prefixEndIndex)
					addStyle(span.item, span.start, spanEnd)
				}
			}
		}
		state._textLines[operation.position.line] = mergeSpanStyles(
			firstLineText,
			prefix.length,
			insertLines.first().toAnnotatedString()
		)

		// Insert middle lines (if any)
		for (i in 1 until insertLines.lastIndex) {
			state.insertLine(
				operation.position.line + i,
				insertLines[i].toAnnotatedString()
			)
		}

		// Handle last line with remainder if there are multiple lines
		if (insertLines.size > 1) {
			val lastInsertedLine = insertLines.last()
			val lastLine = buildAnnotatedString {
				append(lastInsertedLine)
				append(suffix)

				// Add spans that belong to the suffix, adjusting their positions
				currentLine.spanStyles.forEach { span ->
					if (span.end > prefixEndIndex) {
						val adjustedStart =
							maxOf(span.start - prefixEndIndex, 0) + lastInsertedLine.length
						val adjustedEnd = span.end - prefixEndIndex + lastInsertedLine.length
						addStyle(span.item, adjustedStart, adjustedEnd)
					}
				}
			}

			state.insertLine(
				operation.position.line + insertLines.lastIndex,
				lastLine
			)
		}
	}

	private fun applyReplace(
		addToHistory: Boolean,
		operation: TextEditOperation.Replace
	): OperationMetadata? {
		val metadata = if (addToHistory) {
			state.captureMetadata(operation.range)
		} else {
			return null
		}

		when {
			operation.range.isSingleLine() -> {
				val line = state._textLines[operation.range.start.line]
				state._textLines[operation.range.start.line] = handleReplace(
					line,
					operation.range.start.char,
					operation.range.end.char,
					operation.newText
				)
			}

			else -> {
				// Handle multi-line replacement
				val newContent = handleMultiLineReplace(
					state,
					operation.range,
					operation.newText,
				)

				// Remove all affected lines
				state.removeLines(
					operation.range.start.line,
					operation.range.end.line - operation.range.start.line + 1
				)

				// Insert the new content at the start line
				state.insertLine(operation.range.start.line, newContent)
			}
		}
		return metadata
	}

	private fun applyDelete(
		addToHistory: Boolean,
		operation: TextEditOperation.Delete
	): OperationMetadata? {
		val metadata = if (addToHistory) {
			state.captureMetadata(operation.range)
		} else {
			null
		}

		when {
			operation.range.isSingleLine() -> {
				val line = state._textLines[operation.range.start.line]
				val safeStart = operation.range.start.char.coerceIn(0, line.text.length)
				val safeEnd = operation.range.end.char.coerceIn(safeStart, line.text.length)

				state._textLines[operation.range.start.line] = handleDelete(
					line,
					safeStart,
					safeEnd
				)
			}

			else -> {
				handleMultiLineDelete(operation)
			}
		}
		return metadata
	}

	private fun handleMultiLineReplace(
		state: TextEditorState,
		range: TextRange,
		newText: AnnotatedString,
	): AnnotatedString = buildAnnotatedString {
		// First, get all the text content we'll end up with
		val firstLinePrefix = state.textLines[range.start.line].text.substring(0, range.start.char)
		val lastLineSuffix = if (range.end.line < state.textLines.size) {
			state.textLines[range.end.line].text.substring(range.end.char)
		} else ""

		// Build our final text content
		append(firstLinePrefix)
		append(newText.text)
		append(lastLineSuffix)

		// Collect spans from all affected lines
		val allSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

		// Add spans from first line, adjusting if needed
		val firstLine = state.textLines[range.start.line]
		firstLine.spanStyles.forEach { span ->
			when {
				// Span ends before the replacement, keep as is
				span.end <= range.start.char -> {
					allSpans.add(span)
				}
				// Span starts before replacement but extends into it - truncate at replacement start
				span.start < range.start.char -> {
					allSpans.add(
						AnnotatedString.Range(
							span.item,
							span.start,
							range.start.char
						)
					)
				}
				// Span starts within replacement - skip it
			}
		}

		// Add spans from the inserted text
		newText.spanStyles.forEach { span ->
			allSpans.add(
				AnnotatedString.Range(
					span.item,
					span.start + range.start.char,
					span.end + range.start.char
				)
			)
		}

		// Add spans from last line, adjusting positions
		if (range.end.line < state.textLines.size) {
			val lastLine = state.textLines[range.end.line]
			val newBaseOffset = firstLinePrefix.length + newText.length

			lastLine.spanStyles.forEach { span ->
				when {
					// Span starts after the replacement, adjust position fully
					span.start >= range.end.char -> {
						allSpans.add(
							AnnotatedString.Range(
								span.item,
								span.start - range.end.char + newBaseOffset,
								span.end - range.end.char + newBaseOffset
							)
						)
					}
					// Span starts before end of replacement but extends beyond - preserve the part after
					span.end > range.end.char -> {
						allSpans.add(
							AnnotatedString.Range(
								span.item,
								newBaseOffset,  // Start at the insertion point
								span.end - range.end.char + newBaseOffset
							)
						)
					}
					// Span is entirely within replacement - skip it
				}
			}
		}

		// Process all spans through SpanManager to handle any overlaps/merging
		val spanManager = SpanManager()
		val processedSpans = spanManager.processSpans(
			originalText = buildAnnotatedString {
				// Build the same text content explicitly
				append(firstLinePrefix)
				append(newText.text)
				append(lastLineSuffix)

				allSpans.forEach { span ->
					addStyle(span.item, span.start, span.end)
				}
			}
		)

		// Apply the processed spans
		processedSpans.forEach { span ->
			addStyle(span.item, span.start, span.end)
		}
	}

	private fun handleMultiLineDelete(operation: TextEditOperation.Delete) {
		// Add bounds checking for line indices
		val startLine = operation.range.start.line.coerceIn(0, state._textLines.lastIndex)
		val endLine = operation.range.end.line.coerceIn(0, state._textLines.lastIndex)

		if (startLine > endLine || state._textLines.isEmpty()) {
			// If trying to delete from empty state, ensure we maintain one empty line
			if (state._textLines.isEmpty()) {
				state._textLines.add(AnnotatedString(""))
			}
			return
		}

		// Store the deleted content for undo
		val firstLine = state._textLines[startLine]
		val lastLine = state._textLines[endLine]

		// Safe character bounds
		val startChar = operation.range.start.char.coerceIn(0, firstLine.text.length)
		val endChar = operation.range.end.char.coerceIn(0, lastLine.text.length)

		// If deleting everything, maintain one empty line
		if (startLine == 0 && endLine == state._textLines.lastIndex &&
			startChar == 0 && endChar == lastLine.text.length
		) {
			state._textLines.clear()
			state._textLines.add(AnnotatedString(""))
		} else {
			// Normal multi-line delete with previous implementation...
			val startText = firstLine.text.substring(0, startChar)
			val endText = lastLine.text.substring(endChar)

			state.removeLines(startLine, endLine - startLine + 1)

			state.insertLine(
				startLine,
				buildAnnotatedStringWithSpans { addSpan ->
					append(startText)
					append(endText)

					// Handle spans with same bounds checking as above
					firstLine.spanStyles.forEach { span ->
						if (span.end <= startChar) {
							addSpan(span.item, span.start, span.end)
						}
					}

					val startLength = startText.length
					lastLine.spanStyles.forEach { span ->
						if (span.start >= endChar) {
							addSpan(
								span.item,
								span.start - endChar + startLength,
								span.end - endChar + startLength
							)
						}
					}
				})
		}
	}

	fun undo() {
		history.undo()?.let { entry ->
			when (entry.operation) {
				is TextEditOperation.Insert -> undoInsert(entry.operation, entry)
				is TextEditOperation.Delete -> undoDelete(entry, entry.operation)
				is TextEditOperation.Replace -> undoReplace(entry.operation, entry)
			}
		}
	}

	private fun undoReplace(
		operation: TextEditOperation.Replace,
		entry: HistoryEntry
	) {
		println("Undoing Replace:")
		println("Original range: ${operation.range}")
		println("Restoring text: ${operation.oldText}")
		println("Metadata: ${entry.metadata}")

		applyOperation(
			TextEditOperation.Replace(
				range = operation.range,
				oldText = operation.newText,
				newText = operation.oldText,
				cursorBefore = entry.operation.cursorAfter,
				cursorAfter = entry.operation.cursorBefore
			),
			addToHistory = false
		)

		restorePreservedSpans(
			entry.metadata.preservedSpans,
			operation.range.start
		)
	}

	private fun undoDelete(
		entry: HistoryEntry,
		operation: TextEditOperation.Delete
	) {
		entry.metadata.deletedText?.let { deletedText ->
			val insertOperation = TextEditOperation.Insert(
				position = operation.range.start,
				text = deletedText,
				cursorBefore = entry.operation.cursorAfter,
				cursorAfter = entry.operation.cursorBefore
			)
			applyOperation(insertOperation, addToHistory = false)

			restorePreservedSpans(
				entry.metadata.preservedSpans,
				operation.range.start
			)
		}
	}

	private fun undoInsert(
		operation: TextEditOperation.Insert,
		entry: HistoryEntry
	) {
		val endPosition = if (operation.text.contains('\n')) {
			val lines = operation.text.text.split('\n')
			val lastLineLength = lines.last().length
			CharLineOffset(
				operation.position.line + lines.size - 1,
				if (lines.size == 1) operation.position.char + lastLineLength else lastLineLength
			)
		} else {
			CharLineOffset(
				operation.position.line,
				operation.position.char + operation.text.length
			)
		}

		val range = TextRange(operation.position, endPosition)
		applyOperation(
			TextEditOperation.Delete(
				range = range,
				cursorBefore = entry.operation.cursorAfter,
				cursorAfter = entry.operation.cursorBefore,
			),
			addToHistory = false
		)
	}

	fun redo() {
		history.redo()?.let { entry ->
			applyOperation(entry.operation, addToHistory = false)
		}
	}

	private fun restorePreservedSpans(
		preservedSpans: List<PreservedSpan>,
		insertPosition: CharLineOffset
	) {
		preservedSpans.forEach { preserved ->
			val startPos = CharLineOffset(
				line = insertPosition.line + preserved.relativeStart.lineDiff,
				char = if (preserved.relativeStart.lineDiff == 0)
					insertPosition.char + preserved.relativeStart.char
				else
					preserved.relativeStart.char
			)

			val endPos = CharLineOffset(
				line = insertPosition.line + preserved.relativeEnd.lineDiff,
				char = if (preserved.relativeEnd.lineDiff == 0)
					insertPosition.char + preserved.relativeEnd.char
				else
					preserved.relativeEnd.char
			)

			state.richSpanManager.addSpan(startPos, endPos, preserved.style)
		}
	}

	private fun buildAnnotatedStringWithSpans(
		block: AnnotatedString.Builder.(addSpan: (Any, Int, Int) -> Unit) -> Unit
	): AnnotatedString {
		return buildAnnotatedString {
			// Track spans by style to detect overlaps
			val spansByStyle = mutableMapOf<Any, MutableSet<IntRange>>()

			fun addSpanIfNew(item: Any, start: Int, end: Int) {
				val ranges = spansByStyle.getOrPut(item) { mutableSetOf() }

				// Check for overlaps
				val overlapping = ranges.filter { range ->
					start <= range.last + 1 && end >= range.first - 1
				}

				if (overlapping.isNotEmpty()) {
					// Remove overlapping ranges
					ranges.removeAll(overlapping.toSet())

					// Create one merged range
					val newStart = minOf(start, overlapping.minOf { it.first })
					val newEnd = maxOf(end, overlapping.maxOf { it.last })

					ranges.add(newStart..newEnd)
					addStyle(item as SpanStyle, newStart, newEnd)
				} else {
					// No overlap - add new range
					ranges.add(start..end)
					addStyle(item as SpanStyle, start, end)
				}
			}

			block(::addSpanIfNew)
		}
	}

	private fun mergeSpanStyles(
		original: AnnotatedString,
		insertionIndex: Int,
		newText: AnnotatedString
	): AnnotatedString = buildAnnotatedString {
		// First, append all text
		append(original.text.substring(0, insertionIndex))
		append(newText)
		if (insertionIndex < original.length) {
			append(original.text.substring(insertionIndex))
		}

		// Process and condense spans with proper bounds checking
		val processedSpans = spanManager.processSpans(
			originalText = original,
			insertionPoint = insertionIndex,
			insertedText = newText
		)

		// Add processed spans to the result with bounds validation
		processedSpans.forEach { span ->
			val safeStart = span.start.coerceIn(0, length)
			val safeEnd = span.end.coerceIn(safeStart, length)
			if (safeEnd > safeStart) {
				addStyle(span.item, safeStart, safeEnd)
			}
		}
	}

	fun applySpanStyle(textRange: TextRange, spanStyle: SpanStyle) {
		// Get the affected line indices
		val startLineInfo = state.getWrappedLine(textRange.start)
		val endLineInfo = state.getWrappedLine(textRange.end)

		// Handle single line case
		if (startLineInfo.line == endLineInfo.line) {

			applySingleLineSpanStyle(
				lineIndex = startLineInfo.line,
				start = textRange.start.toCharacterIndex(state) - startLineInfo.wrapStartsAtIndex,
				end = textRange.end.toCharacterIndex(state) - startLineInfo.wrapStartsAtIndex,
				spanStyle = spanStyle
			)
			return
		}

		// Handle multi-line case
		for (lineIndex in startLineInfo.line..endLineInfo.line) {
			when (lineIndex) {
				startLineInfo.line -> {
					// First line: from start to end of line
					val lineStart =
						textRange.start.toCharacterIndex(state) - startLineInfo.wrapStartToCharacterIndex(
							state
						)
					val lineEnd = state.getLine(lineIndex).length
					applySingleLineSpanStyle(lineIndex, lineStart, lineEnd, spanStyle)
				}

				endLineInfo.line -> {
					// Last line: from start of line to end
					val lineEnd =
						textRange.end.toCharacterIndex(state) - endLineInfo.wrapStartToCharacterIndex(
							state
						)
					applySingleLineSpanStyle(lineIndex, 0, lineEnd, spanStyle)
				}

				else -> {
					// Middle lines: entire line
					val lineLength = state.getLine(lineIndex).length
					applySingleLineSpanStyle(lineIndex, 0, lineLength, spanStyle)
				}
			}
		}
	}

	private fun applySingleLineSpanStyle(
		lineIndex: Int,
		start: Int,
		end: Int,
		spanStyle: SpanStyle
	) {
		val line = state.getLine(lineIndex)
		val existingSpans = line.spanStyles

		// Create new span list
		val newSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

		// Handle existing spans
		for (existing in existingSpans) {
			when {
				// Existing span is completely before new span
				existing.end <= start -> {
					newSpans.add(existing)
				}
				// Existing span is completely after new span
				existing.start >= end -> {
					newSpans.add(existing)
				}
				// Spans overlap - merge styles
				else -> {
					// Handle the part before overlap
					if (existing.start < start) {
						newSpans.add(
							AnnotatedString.Range(
								existing.item,
								existing.start,
								start
							)
						)
					}

					// Handle the overlapping part
					val overlapStart = maxOf(existing.start, start)
					val overlapEnd = minOf(existing.end, end)
					newSpans.add(
						AnnotatedString.Range(
							existing.item.merge(spanStyle),
							overlapStart,
							overlapEnd
						)
					)

					// Handle the part after overlap
					if (existing.end > end) {
						newSpans.add(
							AnnotatedString.Range(
								existing.item,
								end,
								existing.end
							)
						)
					}
				}
			}
		}

		// Add the new span for any uncovered regions
		val coveredRanges = newSpans.filter { it.start <= end && it.end >= start }
		if (coveredRanges.isEmpty()) {
			newSpans.add(AnnotatedString.Range(spanStyle, start, end))
		}

		// Sort spans by start position
		val sortedSpans = newSpans.sortedBy { it.start }

		// Create new AnnotatedString with updated spans
		val newText = AnnotatedString(
			text = line.text,
			spanStyles = sortedSpans
		)

		// Update the line in state
		state.updateLine(lineIndex, newText)
	}

	fun removeStyleSpan(range: TextRange, style: SpanStyle) {
		TODO("Not yet implemented")
	}
}