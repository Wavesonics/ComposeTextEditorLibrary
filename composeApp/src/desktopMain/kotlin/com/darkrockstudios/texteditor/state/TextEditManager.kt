package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextRange
import com.darkrockstudios.texteditor.annotatedstring.subSequence
import com.darkrockstudios.texteditor.annotatedstring.toAnnotatedString

class TextEditManager(private val state: TextEditorState) {
	private val history = TextEditHistory()

	private fun mergeSpanStyles(
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

	fun applyOperation(operation: TextEditOperation) {
		when (operation) {
			is TextEditOperation.Insert -> {
				if (operation.text.contains('\n')) {
					// Handle multiline insert
					val lines = operation.text.split('\n')

					// Split current line at insertion point
					val currentLine = state._textLines[operation.position.line]
					val beforeInsert = currentLine.subSequence(0, operation.position.char)
					val afterInsert = currentLine.subSequence(operation.position.char)

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
							append(afterInsert)
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
					operation.range.start.line == operation.range.end.line -> {
						val line = state._textLines[operation.range.start.line]
						state._textLines[operation.range.start.line] = buildAnnotatedString {
							// Preserve spans by using subSequence
							append(line.subSequence(0, operation.range.start.char))
							append(line.subSequence(operation.range.end.char))

							// Add all spans that don't intersect with the deleted region
							line.spanStyles.forEach { span ->
								when {
									// Span ends before deletion - keep as is
									span.end <= operation.range.start.char -> {
										addStyle(span.item, span.start, span.end)
									}
									// Span starts after deletion - shift back by deleted length
									span.start >= operation.range.end.char -> {
										val deleteLength =
											operation.range.end.char - operation.range.start.char
										addStyle(
											span.item,
											span.start - deleteLength,
											span.end - deleteLength
										)
									}
									// Span crosses deletion boundaries - preserve and adjust
									span.start < operation.range.start.char && span.end > operation.range.end.char -> {
										val deleteLength =
											operation.range.end.char - operation.range.start.char
										addStyle(span.item, span.start, span.end - deleteLength)
									}
								}
							}
						}
					}
					// Multi-line deletion
					else -> {
						// Handle first line
						val firstLine = state._textLines[operation.range.start.line]
						val startText = firstLine.subSequence(0, operation.range.start.char)

						// Handle last line
						val lastLine = state._textLines[operation.range.end.line]
						val endText = lastLine.subSequence(operation.range.end.char)

						// Remove all lines in the range
						state.removeLines(
							operation.range.start.line,
							operation.range.end.line - operation.range.start.line + 1
						)

						// Merge the remaining text with proper span handling
						state.insertLine(
							operation.range.start.line,
							buildAnnotatedString {
								append(startText)
								append(endText)

								// Preserve spans from first line
								firstLine.spanStyles.forEach { span ->
									if (span.end <= operation.range.start.char) {
										addStyle(span.item, span.start, span.end)
									}
								}

								// Preserve spans from last line, shifting them appropriately
								val startLength = startText.length
								lastLine.spanStyles.forEach { span ->
									if (span.start >= operation.range.end.char) {
										val newStart =
											span.start - operation.range.end.char + startLength
										val newEnd =
											span.end - operation.range.end.char + startLength
										addStyle(span.item, newStart, newEnd)
									}
								}
							}
						)
					}
				}
			}

			is TextEditOperation.Replace -> {
				// Handle replace operation
				when {
					operation.range.isSingleLine() -> {
						val line = state._textLines[operation.range.start.line]
						state._textLines[operation.range.start.line] = buildAnnotatedString {
							// Append text before replacement
							append(line.subSequence(0, operation.range.start.char))
							// Append new text
							append(operation.newText)
							// Append text after replacement
							append(line.subSequence(operation.range.end.char))

							// Handle all existing spans
							line.spanStyles.forEach { span ->
								val replacementLength =
									operation.range.end.char - operation.range.start.char
								val lengthDiff = operation.newText.length - replacementLength

								when {
									// Case 1: Span ends before replacement - keep unchanged
									span.end <= operation.range.start.char -> {
										addStyle(span.item, span.start, span.end)
									}

									// Case 2: Span starts after replacement - shift by length difference
									span.start >= operation.range.end.char -> {
										addStyle(
											span.item,
											span.start + lengthDiff,
											span.end + lengthDiff
										)
									}

									// Case 3: Span contains the replacement - extend over new text
									span.start <= operation.range.start.char &&
											span.end >= operation.range.end.char -> {
										addStyle(
											span.item,
											span.start,
											span.end + lengthDiff
										)
									}

									// Case 4: Span starts before and ends inside replacement
									span.start < operation.range.start.char &&
											span.end > operation.range.start.char &&
											span.end <= operation.range.end.char -> {
										addStyle(
											span.item,
											span.start,
											operation.range.start.char + operation.newText.length
										)
									}

									// Case 5: Span starts inside and ends after replacement
									span.start >= operation.range.start.char &&
											span.start < operation.range.end.char &&
											span.end > operation.range.end.char -> {
										addStyle(
											span.item,
											operation.range.start.char,
											span.end + lengthDiff
										)
									}
								}
							}

							// Add any spans from the new text, adjusted to the insertion position
							operation.newText.spanStyles.forEach { span ->
								addStyle(
									span.item,
									span.start + operation.range.start.char,
									span.end + operation.range.start.char
								)
							}
						}
					}

					else -> {
						// Multi-line replace continues to use delete + insert
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

		// Update cursor position
		state.updateCursorPosition(operation.cursorAfter)

		// Update bookkeeping
		state.updateBookKeeping()

		// Record the operation in history
		history.recordEdit(operation)

		// Notify about content change
		state.notifyContentChanged(operation)
	}

	fun undo() {
		history.undo()?.let { operation ->
			when (operation) {
				is TextEditOperation.Insert -> {
					val range = TextRange(
						operation.position,
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