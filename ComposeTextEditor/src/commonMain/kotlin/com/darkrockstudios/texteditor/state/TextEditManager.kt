package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.annotatedstring.splitAnnotatedString
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
			state._textLines[operation.position.line] = mergeAnnotatedStrings(
				original = line,
				start = operation.position.char,
				newText = operation.text
			)
		}
		return null
	}

	private fun handleDelete(
		line: AnnotatedString,
		start: Int,
		end: Int
	): AnnotatedString = mergeAnnotatedStrings(
		original = line,
		start = start,
		end = end
	)

	private fun handleReplace(
		line: AnnotatedString,
		start: Int,
		end: Int,
		newText: AnnotatedString
	): AnnotatedString = mergeAnnotatedStrings(
		original = line,
		start = start,
		end = end,
		newText = newText
	)


	private fun handleMultiLineInsert(operation: TextEditOperation.Insert) {
		val insertLines = operation.text.splitAnnotatedString()
		val currentLine = state._textLines[operation.position.line]

		// Split current line content
		val prefixEndIndex = operation.position.char.coerceIn(0, currentLine.length)
		val prefix = currentLine.subSequence(0, prefixEndIndex)
		state._textLines[operation.position.line] = mergeAnnotatedStrings(
			original = prefix,
			start = prefix.length,
			newText = insertLines.first()
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
			val suffix = currentLine.subSequence(
				startIndex = prefixEndIndex,
				endIndex = currentLine.length
			)

			val newLastLine = mergeAnnotatedStrings(
				original = lastInsertedLine,
				start = lastInsertedLine.length,
				newText = suffix
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
			null
		}

		when {
			// Single line replacement (no newlines in range or new text)
			operation.range.isSingleLine() && !operation.newText.contains('\n') -> {
				val line = state._textLines[operation.range.start.line]

				// Handle inherited styles if needed
				val inheritedStyles = if (operation.inheritStyle) {
					line.spanStyles.filter { span ->
						span.start <= operation.range.end.char &&
								span.end >= operation.range.start.char
					}.map { it.item }.toSet()
				} else {
					emptySet()
				}

				// Create new text with inherited styles if needed
				val newText = if (inheritedStyles.isNotEmpty()) {
					buildAnnotatedString {
						append(operation.newText)
						inheritedStyles.forEach { style ->
							addStyle(style, 0, operation.newText.length)
						}
					}
				} else {
					operation.newText
				}

				state._textLines[operation.range.start.line] = handleReplace(
					line,
					operation.range.start.char,
					operation.range.end.char,
					newText
				)
			}
			// Multi-line range or replacement text contains newlines
			else -> {
				val newLines = handleMultiLineReplace(
					state,
					operation.range,
					operation.newText,
					operation.inheritStyle
				)

				// Remove all affected lines
				state.removeLines(
					operation.range.start.line,
					operation.range.end.line - operation.range.start.line + 1
				)
				if (newLines.size == 1 && state.isEmpty()) {
					state.updateLine(0, newLines[0])
				} else {
					newLines.forEachIndexed { index, line ->
						if (state.isEmpty() && index == 0) {
							state.updateLine(0, newLines[0])
						} else {
							state.insertLine(operation.range.start.line + index, line)
						}
					}
				}
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
		range: TextEditorRange,
		newText: AnnotatedString,
		inheritStyle: Boolean
	): List<AnnotatedString> {
		// Split the new text into lines if it contains newlines
		val newLines = if (newText.contains('\n')) {
			newText.splitAnnotatedString()
		} else {
			listOf(newText)
		}

		// Validate range bounds
		val startLine = range.start.line.coerceIn(0, state.textLines.lastIndex)
		val endLine = range.end.line.coerceIn(0, state.textLines.lastIndex)

		// Process first line - combine prefix with first new line
		val firstLine = state.textLines[startLine]
		val firstLinePrefix =
			firstLine.subSequence(0, range.start.char.coerceIn(0, firstLine.length))

		// Collect inherited styles if needed
		val inheritedStyles = if (inheritStyle) {
			// Collect styles from the first line that overlap with our range
			firstLine.spanStyles.filter { span ->
				span.start <= range.end.char && span.end >= range.start.char
			}.map { it.item }.toSet()
		} else {
			emptySet()
		}

		// Process last line - get suffix if it exists
		val lastLineSuffix = if (endLine < state.textLines.size) {
			val lastLine = state.textLines[endLine]
			lastLine.subSequence(
				range.end.char.coerceIn(0, lastLine.length),
				lastLine.length
			)
		} else {
			null
		}

		return buildList {
			if (!newText.contains('\n')) {
				// Single-line case
				add(buildAnnotatedString {
					append(firstLinePrefix.text)
					append(newText.text)
					if (lastLineSuffix != null) {
						append(lastLineSuffix.text)
					}

					// Handle styles from prefix
					firstLinePrefix.spanStyles.forEach { span ->
						addStyle(span.item, span.start, span.end)
					}

					val prefixLength = firstLinePrefix.length

					// If inheriting styles, apply them to the new text
					if (inheritStyle) {
						inheritedStyles.forEach { style ->
							addStyle(style, prefixLength, prefixLength + newText.length)
						}
					}

					// Add new text styles on top of inherited styles
					newText.spanStyles.forEach { span ->
						addStyle(span.item, span.start + prefixLength, span.end + prefixLength)
					}

					// Handle styles from suffix
					if (lastLineSuffix != null) {
						val offset = prefixLength + newText.length
						lastLineSuffix.spanStyles.forEach { span ->
							addStyle(span.item, span.start + offset, span.end + offset)
						}
					}
				})
			} else {
				// First line: combine prefix with first line of new text
				add(buildAnnotatedString {
					append(firstLinePrefix.text)
					append(newLines.first().text)

					// Handle styles from prefix
					firstLinePrefix.spanStyles.forEach { span ->
						addStyle(span.item, span.start, span.end)
					}

					val prefixLength = firstLinePrefix.length

					// If inheriting styles, apply them to the first line portion
					if (inheritStyle) {
						inheritedStyles.forEach { style ->
							addStyle(style, prefixLength, prefixLength + newLines.first().length)
						}
					}

					// Add new text styles for first line
					newLines.first().spanStyles.forEach { span ->
						addStyle(span.item, span.start + prefixLength, span.end + prefixLength)
					}
				})

				// Middle lines
				if (newLines.size > 2) {
					// For middle lines, either use their styles or inherit
					newLines.subList(1, newLines.lastIndex).forEach { line ->
						add(buildAnnotatedString {
							append(line.text)

							// If inheriting styles, apply them to the whole line
							if (inheritStyle) {
								inheritedStyles.forEach { style ->
									addStyle(style, 0, line.length)
								}
							}

							// Add line's own styles on top
							line.spanStyles.forEach { span ->
								addStyle(span.item, span.start, span.end)
							}
						})
					}
				}

				// Last line: combine last line of new text with suffix
				if (newLines.size > 1) {
					val lastNewLine = newLines.last()
					add(buildAnnotatedString {
						append(lastNewLine.text)
						if (lastLineSuffix != null) {
							append(lastLineSuffix.text)
						}

						// If inheriting styles, apply them to the last line portion
						if (inheritStyle) {
							inheritedStyles.forEach { style ->
								addStyle(style, 0, lastNewLine.length)
							}
						}

						// Add last line's own styles
						lastNewLine.spanStyles.forEach { span ->
							addStyle(span.item, span.start, span.end)
						}

						// Handle styles from suffix
						if (lastLineSuffix != null) {
							val offset = lastNewLine.length
							lastLineSuffix.spanStyles.forEach { span ->
								addStyle(span.item, span.start + offset, span.end + offset)
							}
						}
					})
				}
			}
		}
	}

	private fun handleMultiLineDelete(operation: TextEditOperation.Delete) {
		// Add bounds checking for line indices
		val startLine = operation.range.start.line.coerceIn(0, state._textLines.lastIndex)
		val endLine = operation.range.end.line.coerceIn(0, state._textLines.lastIndex)

		// Edge case: no lines to delete
		if (startLine > endLine || state._textLines.isEmpty()) {
			if (state._textLines.isEmpty()) {
				state._textLines.add(AnnotatedString(""))
			}
			return
		}

		// Process the first and last lines
		val firstLine = state._textLines[startLine]
		val lastLine = state._textLines[endLine]

		val startChar = operation.range.start.char.coerceIn(0, firstLine.text.length)
		val endChar = operation.range.end.char.coerceIn(0, lastLine.text.length)

		if (startLine == 0 && endLine == state._textLines.lastIndex &&
			startChar == 0 && endChar == lastLine.text.length
		) {
			// If deleting all content, leave one empty line
			state._textLines.clear()
			state._textLines.add(AnnotatedString(""))
		} else {
			val startText = firstLine.text.substring(0, startChar)
			val endText = lastLine.text.substring(endChar)

			// Remove the lines between start and end
			state.removeLines(startLine, endLine - startLine + 1)

			val newText = buildAnnotatedStringWithSpans { addSpan ->
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
			}

			if (state.isEmpty()) {
				state.updateLine(0, newText)
			} else {
				state.insertLine(
					startLine,
					newText
				)
			}
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
		val undoOperation = TextEditOperation.Replace(
			range = operation.range,
			oldText = operation.newText,  // B (current state)
			newText = operation.oldText,  // A (what we're restoring)
			cursorBefore = entry.operation.cursorAfter,
			cursorAfter = entry.operation.cursorBefore,
			inheritStyle = operation.inheritStyle
		)

		// Apply the operation atomically
		applyOperation(undoOperation, addToHistory = false)

		// Restore any preserved rich spans
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

		val range = TextEditorRange(operation.position, endPosition)
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

			state.addRichSpan(startPos, endPos, preserved.style)
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

	internal fun mergeAnnotatedStrings(
		original: AnnotatedString,
		start: Int,
		end: Int = start, // For insertions, start == end
		newText: AnnotatedString? = null
	): AnnotatedString = buildAnnotatedString {
		// Add text outside the affected range
		append(original.text.substring(0, start))
		if (newText != null) append(newText.text)
		append(original.text.substring(end))

		// Process spans with SpanManager
		val processedSpans = spanManager.processSpans(
			originalText = original,
			insertionPoint = if (newText != null) start else -1,
			insertedText = newText,
			deletionStart = if (end > start) start else -1,
			deletionEnd = if (end > start) end else -1
		)

		// Add processed spans to the result
		processedSpans.forEach { span ->
			addStyle(span.item, span.start, span.end)
		}
	}

	fun addSpanStyle(textRange: TextEditorRange, spanStyle: SpanStyle) {
		val operation = TextEditOperation.StyleSpan(
			range = textRange,
			style = spanStyle,
			isAdd = true,
			cursorBefore = state.cursorPosition,
			cursorAfter = state.cursorPosition // Keep cursor in same position
		)
		applyOperation(operation)
	}

	fun removeStyleSpan(textRange: TextEditorRange, spanStyle: SpanStyle) {
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
		val newSpans = mergeOverlaps(existingSpans, spanStyle, start, end)

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

	private fun mergeOverlaps(
		existingSpans: List<AnnotatedString.Range<SpanStyle>>,
		spanStyle: SpanStyle,
		start: Int,
		end: Int
	): List<AnnotatedString.Range<SpanStyle>> {
		val result = mutableListOf<AnnotatedString.Range<SpanStyle>>()
		val spanRanges = mutableListOf<Pair<Int, Int>>()

		// First collect all ranges with matching style
		existingSpans.forEach { span ->
			if (span.item == spanStyle) {
				spanRanges.add(span.start to span.end)
			} else {
				result.add(span)
			}
		}
		// Add the new range
		spanRanges.add(start to end)

		// Sort ranges by start position
		spanRanges.sortBy { it.first }

		// Merge overlapping ranges in a single pass
		var currentStart = spanRanges.firstOrNull()?.first ?: start
		var currentEnd = spanRanges.firstOrNull()?.second ?: end

		for (i in 1 until spanRanges.size) {
			val (nextStart, nextEnd) = spanRanges[i]
			if (nextStart <= currentEnd + 1) {
				// Ranges overlap or are adjacent, extend current range
				currentEnd = maxOf(currentEnd, nextEnd)
			} else {
				// Ranges don't overlap, add current range and start new one
				result.add(AnnotatedString.Range(spanStyle, currentStart, currentEnd))
				currentStart = nextStart
				currentEnd = nextEnd
			}
		}

		// Add final range
		result.add(AnnotatedString.Range(spanStyle, currentStart, currentEnd))

		return result.sortedBy { it.start }
	}
}