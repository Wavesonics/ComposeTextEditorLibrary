package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextRange
import com.darkrockstudios.texteditor.annotatedstring.splitToAnnotatedString
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class TextEditManager(private val state: TextEditorState) {
	private val spanManager = SpanManager()
	internal val history = TextEditHistory()

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
			is TextEditOperation.StyleSpan -> applyStyleOperation(operation)
		}

		state.updateCursorPosition(operation.cursorAfter)
		state.richSpanManager.updateSpans(operation, metadata)
		if (addToHistory) {
			history.recordEdit(operation, metadata ?: OperationMetadata())
		}

		state.updateBookKeeping()
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
		val insertLines = operation.text.splitToAnnotatedString()
		val currentLine = state._textLines[operation.position.line]

		// Split current line content
		val prefixEndIndex = operation.position.char.coerceIn(0, currentLine.length)
		val prefix = currentLine.subSequence(0, prefixEndIndex)

		state._textLines[operation.position.line] = mergeSpanStyles(
			prefix,
			prefix.length,
			insertLines.first()
		)

		// Insert middle lines (if any)
		for (i in 1 until insertLines.lastIndex) {
			state.insertLine(
				operation.position.line + i,
				insertLines[i]
			)
		}

		// Handle last line with remainder if there are multiple lines
		if (insertLines.size > 1) {
			val lastInsertedLine = insertLines.last()
			val suffix = currentLine.subSequence(prefixEndIndex, currentLine.lastIndex)

			val newLastLine = mergeSpanStyles(
				lastInsertedLine,
				lastInsertedLine.length,
				suffix
			)
			state.insertLine(
				operation.position.line + insertLines.lastIndex,
				newLastLine
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
			if (state._textLines.isEmpty()) {
				state._textLines.add(AnnotatedString(""))
			}
			return
		}

		val firstLine = state._textLines[startLine]
		val lastLine = state._textLines[endLine]

		val startChar = operation.range.start.char.coerceIn(0, firstLine.text.length)
		val endChar = operation.range.end.char.coerceIn(0, lastLine.text.length)

		if (startLine == 0 && endLine == state._textLines.lastIndex &&
			startChar == 0 && endChar == lastLine.text.length
		) {
			state._textLines.clear()
			state._textLines.add(AnnotatedString(""))
		} else {
			val startText = firstLine.text.substring(0, startChar)
			val endText = lastLine.text.substring(endChar)

			state.removeLines(startLine, endLine - startLine + 1)

			state.insertLine(
				startLine,
				buildAnnotatedStringWithSpans { addSpan ->
					append(startText)
					append(endText)

					// Handle spans from the first line
					firstLine.spanStyles.forEach { span ->
						when {
							// Span ends before deletion - keep as is
							span.end <= startChar -> {
								addSpan(span.item, span.start, span.end)
							}
							// Span starts before deletion - extend to the new end
							span.start < startChar -> {
								addSpan(span.item, span.start, startChar)
							}
						}
					}

					// Handle spans from the last line
					val startLength = startText.length
					lastLine.spanStyles.forEach { span ->
						when {
							// Span starts after deletion - shift it back
							span.start >= endChar -> {
								addSpan(
									span.item,
									span.start - endChar + startLength,
									span.end - endChar + startLength
								)
							}
							// Span extends past deletion point - preserve the remainder
							span.end > endChar -> {
								addSpan(
									span.item,
									startLength, // Start at the join point
									span.end - endChar + startLength
								)
							}
						}
					}
				})
		}
	}

	private fun applyStyleOperation(operation: TextEditOperation.StyleSpan): OperationMetadata? {
		if (operation.range.isSingleLine()) {
			if (operation.isAdd) {
				applySingleLineSpanStyle(
					lineIndex = operation.range.start.line,
					start = operation.range.start.char,
					end = operation.range.end.char,
					spanStyle = operation.style
				)
			} else {
				removeSingleLineSpanStyle(
					lineIndex = operation.range.start.line,
					start = operation.range.start.char,
					end = operation.range.end.char,
					spanStyle = operation.style
				)
			}
		} else {
			// Handle multi-line case
			val startLine = operation.range.start.line
			val endLine = operation.range.end.line

			for (lineIndex in startLine..endLine) {
				val lineStart = if (lineIndex == startLine) operation.range.start.char else 0
				val lineEnd = if (lineIndex == endLine)
					operation.range.end.char
				else
					state.getLine(lineIndex).length

				if (operation.isAdd) {
					applySingleLineSpanStyle(lineIndex, lineStart, lineEnd, operation.style)
				} else {
					removeSingleLineSpanStyle(lineIndex, lineStart, lineEnd, operation.style)
				}
			}
		}

		return null
	}

	private fun removeSingleLineSpanStyle(
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
				// Span is completely before or after the removal range - keep as is
				existing.end <= start || existing.start >= end -> {
					newSpans.add(existing)
				}
				// Span overlaps with removal range
				else -> {
					// If the styles don't match, keep the span
					if (existing.item != spanStyle) {
						newSpans.add(existing)
						continue
					}

					// Keep portion before removal range
					if (existing.start < start) {
						newSpans.add(
							AnnotatedString.Range(
								existing.item,
								existing.start,
								start
							)
						)
					}

					// Keep portion after removal range
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

		// Create new AnnotatedString with updated spans
		val newText = AnnotatedString(
			text = line.text,
			spanStyles = newSpans.sortedBy { it.start }
		)

		// Update the line in state
		state.updateLine(lineIndex, newText)
	}

	fun undo() {
		history.undo()?.let { entry ->
			when (entry.operation) {
				is TextEditOperation.Insert -> undoInsert(entry.operation, entry)
				is TextEditOperation.Delete -> undoDelete(entry, entry.operation)
				is TextEditOperation.Replace -> undoReplace(entry.operation, entry)
				is TextEditOperation.StyleSpan -> undoStyleSpan(entry.operation)
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

		restorePreservedRichSpans(
			entry.metadata.preservedRichSpans,
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

			restorePreservedRichSpans(
				entry.metadata.preservedRichSpans,
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

	private fun undoStyleSpan(operation: TextEditOperation.StyleSpan) {
		// Create inverse operation - if it was adding a style, we remove it and vice versa
		val inverseOperation = TextEditOperation.StyleSpan(
			range = operation.range,
			style = operation.style,
			isAdd = !operation.isAdd,
			cursorBefore = operation.cursorAfter,
			cursorAfter = operation.cursorBefore
		)

		applyOperation(inverseOperation, addToHistory = false)
	}

	fun redo() {
		history.redo()?.let { entry ->
			applyOperation(entry.operation, addToHistory = false)
		}
	}

	private fun restorePreservedRichSpans(
		preservedRichSpans: List<PreservedRichSpan>,
		insertPosition: CharLineOffset
	) {
		preservedRichSpans.forEach { preserved ->
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

			state.richSpanManager.addRichSpan(startPos, endPos, preserved.style)
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

	fun addSpanStyle(textRange: TextRange, spanStyle: SpanStyle) {
		val operation = TextEditOperation.StyleSpan(
			range = textRange,
			style = spanStyle,
			isAdd = true,
			cursorBefore = state.cursorPosition,
			cursorAfter = state.cursorPosition // Keep cursor in same position
		)
		applyOperation(operation)
	}

	fun removeStyleSpan(textRange: TextRange, spanStyle: SpanStyle) {
		val operation = TextEditOperation.StyleSpan(
			range = textRange,
			style = spanStyle,
			isAdd = false,
			cursorBefore = state.cursorPosition,
			cursorAfter = state.cursorPosition // Keep cursor in same position
		)
		applyOperation(operation)
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
				existing.end < start -> {
					newSpans.add(existing)
				}
				// Existing span is completely after new span
				existing.start > end -> {
					newSpans.add(existing)
				}
				// Spans overlap or are adjacent - merge styles
				else -> {
					// Merge the overlapping or adjacent span
					val mergedStyle = existing.item.merge(spanStyle)
					val mergedStart = minOf(existing.start, start)
					val mergedEnd = maxOf(existing.end, end)
					newSpans.add(
						AnnotatedString.Range(
							mergedStyle,
							mergedStart,
							mergedEnd
						)
					)
				}
			}
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
}