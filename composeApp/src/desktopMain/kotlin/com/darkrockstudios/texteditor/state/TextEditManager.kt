package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.annotatedstring.toAnnotatedString
import com.darkrockstudios.texteditor.toRange

class TextEditManager(private val state: TextEditorState) {
	private val spanManager = SpanManager()
	private val history = TextEditHistory()

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
					ranges.removeAll(overlapping)

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
		append(original.text.substring(insertionIndex))

		// Process and condense spans
		val processedSpans = spanManager.processSpans(
			originalText = original,
			insertionPoint = insertionIndex,
			insertedText = newText
		)

		// Add processed spans to the result
		processedSpans.forEach { span ->
			addStyle(span.item, span.start, span.end)
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

	fun applyOperation(operation: TextEditOperation) {
		when (operation) {
			is TextEditOperation.Insert -> {
				if (operation.text.contains('\n')) {
					// Handle multiline insert
					val lines = operation.text.split('\n')
					val currentLine = state._textLines[operation.position.line]

					// Apply span merging to first line
					state._textLines[operation.position.line] = mergeSpanStyles(
						currentLine,
						operation.position.char,
						lines.first().toAnnotatedString()
					)

					// Insert middle lines
					for (i in 1 until lines.size - 1) {
						state.insertLine(operation.position.line + i, lines[i])
					}

					// Apply span merging to last line with remaining content
					if (lines.size > 1) {
						val lastLineText = buildAnnotatedString {
							append(lines.last())
							append(currentLine.text.substring(operation.position.char))
						}
						state.insertLine(
							operation.position.line + lines.size - 1,
							lastLineText
						)
					}
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
						// For multi-line replacements, use delete + insert
						val deleteOp = TextEditOperation.Delete(
							range = operation.range,
							deletedText = operation.oldText,
							cursorBefore = operation.cursorBefore,
							cursorAfter = operation.range.start
						)
						applyOperation(deleteOp)

						val insertOp = TextEditOperation.Insert(
							position = operation.range.start,
							text = operation.newText,
							cursorBefore = operation.range.start,
							cursorAfter = operation.cursorAfter
						)
						applyOperation(insertOp)
						return
					}
				}
			}
		}

		state.updateCursorPosition(operation.cursorAfter)
		state.updateBookKeeping()
		history.recordEdit(operation)
		state.notifyContentChanged(operation)
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
						)
					)
				}
				is TextEditOperation.Delete -> {
					applyOperation(
						TextEditOperation.Insert(
							position = operation.range.start,
							text = operation.deletedText,
							cursorBefore = operation.cursorAfter,
							cursorAfter = operation.cursorBefore
						)
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
						)
					)
				}
			}
		}
	}

	fun redo() {
		history.redo()?.let { operation ->
			applyOperation(operation)
		}
	}
}