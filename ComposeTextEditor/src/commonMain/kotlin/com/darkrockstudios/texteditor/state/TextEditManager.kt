package com.darkrockstudios.texteditor.state

import androidx.annotation.VisibleForTesting
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
		// Extract prefix from the first line
		val firstLine = state.textLines[range.start.line]
		val prefix =
			firstLine.subSequence(0, range.start.char.coerceIn(0, firstLine.length)).ifEmpty {
				AnnotatedString("")
			}

		val inheritedStyles = if (inheritStyle) {
			getStyles(range)
		} else {
			emptySet()
		}

		// Extract suffix from the last line
		val suffix = if (range.end.line < state.textLines.size) {
			val lastLine = state.textLines[range.end.line]
			lastLine.subSequence(range.end.char.coerceIn(0, lastLine.length), lastLine.length)
				.ifEmpty {
					AnnotatedString("")
				}
		} else {
			AnnotatedString("")
		}

		return if (newText.contains('\n')) {
			val newLines = if (inheritStyle) {
				buildAnnotatedStringWithSpans { addSpan ->
					append(newText.text)
					inheritedStyles.forEach { style ->
						addSpan(style, 0, newText.lastIndex)
					}
				}.splitAnnotatedString()
			} else {
				newText.splitAnnotatedString()
			}

			buildList {
				add(appendAnnotatedStrings(prefix, newLines.first()))

				(1..<newLines.lastIndex).forEach { newLineIndex ->
					add(newLines[newLineIndex])
				}

				add(appendAnnotatedStrings(newLines.last(), suffix))
			}
		} else {
			listOf(
				buildAnnotatedString {
					append(prefix)
					if (inheritStyle) {
						buildAnnotatedStringWithSpans { addSpan ->
							append(newText.text)
							inheritedStyles.forEach { style ->
								addSpan(style, 0, newText.lastIndex)
							}
						}
					} else {
						append(newText)
					}
					append(suffix)
				}
			)
		}
	}

	private fun getStyles(range: TextEditorRange): Set<SpanStyle> {
		val firstLine = state.textLines[range.start.line]
		// Collect styles from all affected lines that overlap with our range
		return buildSet {
			// First line styles
			addAll(firstLine.spanStyles
				.filter { span -> span.end > range.start.char }
				.map { it.item })

			// Middle lines styles (if any)
			(range.start.line + 1 until range.end.line).forEach { lineIndex ->
				addAll(state.textLines[lineIndex].spanStyles.map { it.item })
			}

			// Last line styles (if different from first line)
			if (range.end.line > range.start.line && range.end.line < state.textLines.size) {
				addAll(state.textLines[range.end.line].spanStyles
					.filter { span -> span.start < range.end.char }
					.map { it.item })
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
				val updatedLine = spanManager.applySingleLineSpanStyle(
					line = state.textLines[operation.range.start.line],
					start = operation.range.start.char,
					end = operation.range.end.char,
					spanStyle = operation.style
				)
				state.updateLine(operation.range.start.line, updatedLine)
			} else {
				val updatedLine = spanManager.removeSingleLineSpanStyle(
					line = state.textLines[operation.range.start.line],
					start = operation.range.start.char,
					end = operation.range.end.char,
					spanStyle = operation.style
				)
				state.updateLine(operation.range.start.line, updatedLine)
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
					val updatedLine = spanManager.applySingleLineSpanStyle(
						state.textLines[lineIndex],
						lineStart,
						lineEnd,
						operation.style
					)
					state.updateLine(lineIndex, updatedLine)
				} else {
					val updatedLine = spanManager.removeSingleLineSpanStyle(
						state.textLines[lineIndex],
						lineStart,
						lineEnd,
						operation.style
					)
					state.updateLine(lineIndex, updatedLine)
				}
			}
		}

		return null
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
		// Calculate the current range of the replaced text
		val undoRange = if (operation.newText.contains('\n')) {
			// For any multi-line new text, calculate the current range it occupies
			val newLines = operation.newText.text.split('\n')
			TextEditorRange(
				start = operation.range.start,
				end = CharLineOffset(
					operation.range.start.line + newLines.size - 1,
					if (newLines.size == 1)
						operation.range.start.char + operation.newText.length
					else
						newLines.last().length
				)
			)
		} else {
			// For single-line replacements, adjust the end position based on length difference
			TextEditorRange(
				start = operation.range.start,
				end = CharLineOffset(
					operation.range.start.line,
					operation.range.start.char + operation.newText.length
				)
			)
		}

		val undoOperation = TextEditOperation.Replace(
			range = undoRange,
			oldText = operation.newText,  // B (current state)
			newText = operation.oldText,  // A (what we're restoring)
			cursorBefore = entry.operation.cursorAfter,
			cursorAfter = entry.operation.cursorBefore,
			inheritStyle = false
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
		block: AnnotatedString.Builder.(addSpan: (SpanStyle, Int, Int) -> Unit) -> Unit
	): AnnotatedString {
		return buildAnnotatedString {
			// Track spans by style to detect overlaps
			val spansByStyle = mutableMapOf<Any, MutableSet<IntRange>>()

			fun addSpanIfNew(item: SpanStyle, start: Int, end: Int) {
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
					addStyle(item, newStart, newEnd)
				} else {
					// No overlap - add new range
					ranges.add(start..end)
					addStyle(item, start, end)
				}
			}

			block(::addSpanIfNew)
		}
	}

	@VisibleForTesting
	internal fun combineAnnotatedStrings(
		prefix: AnnotatedString,
		center: AnnotatedString,
		suffix: AnnotatedString,
	): AnnotatedString {
		val temp = appendAnnotatedStrings(prefix, center)
		return appendAnnotatedStrings(temp, suffix)
	}

	@VisibleForTesting
	internal fun appendAnnotatedStrings(
		original: AnnotatedString,
		newText: AnnotatedString
	): AnnotatedString = mergeAnnotatedStrings(
		original = original,
		start = original.length,
		end = original.length,
		newText = newText
	)

	@VisibleForTesting
	internal fun prependAnnotatedStrings(
		original: AnnotatedString,
		newText: AnnotatedString
	): AnnotatedString = mergeAnnotatedStrings(
		original = original,
		start = 0,
		end = 0,
		newText = newText
	)

	@VisibleForTesting
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
}