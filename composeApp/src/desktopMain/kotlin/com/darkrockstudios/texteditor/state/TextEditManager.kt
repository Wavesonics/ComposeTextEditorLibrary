package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextRange
import com.darkrockstudios.texteditor.annotatedstring.toAnnotatedString
import com.darkrockstudios.texteditor.toRange
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

	private fun handleDelete(
		line: AnnotatedString,
		start: Int,
		end: Int
	): AnnotatedString = buildAnnotatedString {
		// Append text without deleted portion
		append(line.text.substring(0, start))
		append(line.text.substring(end))

		// Process spans using SpanManager
		val spanManager = SpanManager()
		val processedSpans = spanManager.processSpans(
			originalText = line,
			deletionStart = start,
			deletionEnd = end
		)

		// Add processed spans to the result
		processedSpans.forEach { span ->
			addStyle(span.item, span.start, span.end)
		}
	}

	private fun handleReplace(
		line: AnnotatedString,
		start: Int,
		end: Int,
		newText: AnnotatedString
	): AnnotatedString = buildAnnotatedString {
		// Append text portions
		append(line.text.substring(0, start))
		append(newText)
		append(line.text.substring(end))

		// Process spans using SpanManager
		val spanManager = SpanManager()
		val processedSpans = spanManager.processSpans(
			originalText = line,
			deletionStart = start,
			deletionEnd = end,
			insertionPoint = start,
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
		val prefixEndIndex = operation.position.char
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
						val adjustedStart = maxOf(span.start - prefixEndIndex, 0) + lastInsertedLine.length
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

	fun applyOperation(operation: TextEditOperation, addToHistory: Boolean = true) {
		when (operation) {
			is TextEditOperation.Insert -> {
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
			}

			is TextEditOperation.Delete -> {
				when {
					// Single line deletion
					operation.range.isSingleLine() -> {
						val line = state._textLines[operation.range.start.line]
						state._textLines[operation.range.start.line] = handleDelete(
							line,
							operation.range.start.char,
							operation.range.end.char
						)
					}
					// Multi-line deletion
					else -> {
						// Handle first line
						val firstLine = state._textLines[operation.range.start.line]
						val startText = firstLine.text.substring(0, operation.range.start.char)

						// Handle last line
						val lastLine = state._textLines[operation.range.end.line]
						val endText = lastLine.text.substring(operation.range.end.char)

						// Remove all lines in the range
						state.removeLines(
							operation.range.start.line,
							operation.range.end.line - operation.range.start.line + 1
						)

						// Merge the remaining text with proper span handling
						state.insertLine(
							operation.range.start.line,
							buildAnnotatedStringWithSpans { addSpan ->
								append(startText)
								append(endText)

								// Handle spans from first line
								firstLine.spanStyles.forEach { span ->
									if (span.end <= operation.range.start.char) {
										addSpan(span.item, span.start, span.end)
									}
								}

								// Handle spans from last line
								val startLength = startText.length
								lastLine.spanStyles.forEach { span ->
									if (span.start >= operation.range.end.char) {
										addSpan(
											span.item,
											span.start - operation.range.end.char + startLength,
											span.end - operation.range.end.char + startLength
										)
									}
								}
							}
						)
					}
				}
			}

			is TextEditOperation.Replace -> {
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
			}
		}

		state.updateCursorPosition(operation.cursorAfter)
		state.richSpanManager.updateSpans(operation)
		state.updateBookKeeping()
		if (addToHistory) {
			history.recordEdit(operation)
		}
		state.notifyContentChanged()

		_editOperations.tryEmit(operation)
	}

	fun undo() {
		history.undo()?.let { operation ->
			when (operation) {
				is TextEditOperation.Insert -> {
					val range = operation.position.toRange(
						CharLineOffset(
							operation.position.line,
							operation.position.char + operation.text.length
						)
					)
					applyOperation(
						TextEditOperation.Delete(
							range = range,
							deletedText = operation.text,
							cursorBefore = operation.cursorAfter,
							cursorAfter = operation.cursorBefore
						),
						addToHistory = false
					)
				}
				is TextEditOperation.Delete -> {
					applyOperation(
						TextEditOperation.Insert(
							position = operation.range.start,
							text = operation.deletedText,
							cursorBefore = operation.cursorAfter,
							cursorAfter = operation.cursorBefore
						),
						addToHistory = false
					)
				}
				is TextEditOperation.Replace -> {
					applyOperation(
						TextEditOperation.Replace(
							range = operation.range,
							oldText = operation.newText,
							newText = operation.oldText,
							cursorBefore = operation.cursorAfter,
							cursorAfter = operation.cursorBefore
						),
						addToHistory = false
					)
				}
			}
		}
	}

	fun redo() {
		history.redo()?.let { operation ->
			applyOperation(operation, addToHistory = false)
		}
	}
}