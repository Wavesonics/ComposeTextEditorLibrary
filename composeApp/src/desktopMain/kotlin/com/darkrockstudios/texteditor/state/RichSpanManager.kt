package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.richstyle.RichSpanStyle

class RichSpanManager(
	private val state: TextEditorState
) {
	private val spans = mutableListOf<RichSpan>()

	fun addSpan(start: CharLineOffset, end: CharLineOffset, style: RichSpanStyle) {
		spans.add(RichSpan(start, end, style))
	}

	fun removeSpan(span: RichSpan) {
		spans.remove(span)
	}

	fun getSpansForLineWrap(lineWrap: LineWrap): List<RichSpan> {
		return spans.filter { it.intersectsWith(lineWrap) }
	}

	fun updateSpans(operation: TextEditOperation) {
		val updatedSpans = mutableListOf<RichSpan>()

		spans.forEach { span ->
			when (operation) {
				is TextEditOperation.Insert -> {
					if (operation.text.text == "\n") {
						// Special handling for newline insertion
						when {
							// Case 1: Newline inserted before the span
							operation.position.line < span.start.line ||
									(operation.position.line == span.start.line && operation.position.char <= span.start.char) -> {
								// Move entire span to new line
								val newStart = operation.transformOffset(span.start, state)
								val newEnd = operation.transformOffset(span.end, state)
								updatedSpans.add(span.copy(start = newStart, end = newEnd))
							}

							// Case 2: Newline inserted inside the span
							operation.position.line == span.start.line &&
									operation.position.char > span.start.char &&
									operation.position.char < span.end.char -> {
								// Calculate remaining length in original span after split point
								val remainingLength = span.end.char - operation.position.char

								// First part remains on original line
								updatedSpans.add(
									span.copy(
										end = CharLineOffset(
											span.start.line,
											operation.position.char
										)
									)
								)

								// Second part moves to new line
								// Only spans the remaining length from the original span
								updatedSpans.add(
									span.copy(
										start = CharLineOffset(span.start.line + 1, 0),
										end = CharLineOffset(span.start.line + 1, remainingLength)
									)
								)
							}

							// Case 3: Newline inserted after the span
							operation.position.line > span.start.line ||
									(operation.position.line == span.start.line && operation.position.char >= span.end.char) -> {
								// Keep span as is
								updatedSpans.add(span)
							}
						}
					} else {
						// Regular text insertion
						val newStart = operation.transformOffset(span.start, state)
						val newEnd = operation.transformOffset(span.end, state)
						updatedSpans.add(span.copy(start = newStart, end = newEnd))
					}
				}

				is TextEditOperation.Delete -> {
					if (operation.deletedText.text == "\n") {
						// Special handling for newline deletion
						val deletionPoint = operation.range.start
						val nextLineStart = operation.range.end

						when {
							// Span is entirely before the deletion point on the first line
							span.end.line < deletionPoint.line ||
									(span.end.line == deletionPoint.line && span.end.char <= deletionPoint.char) -> {
								updatedSpans.add(span)
							}
							// Span is entirely on the second line
							span.start.line == nextLineStart.line -> {
								val newStart = CharLineOffset(
									deletionPoint.line,
									deletionPoint.char + span.start.char
								)
								val newEnd = CharLineOffset(
									deletionPoint.line,
									deletionPoint.char + span.end.char
								)
								updatedSpans.add(span.copy(start = newStart, end = newEnd))
							}
							// Span crosses the newline
							span.start.line == deletionPoint.line &&
									span.end.line == nextLineStart.line -> {
								val newEnd = CharLineOffset(
									deletionPoint.line,
									deletionPoint.char + span.end.char
								)
								updatedSpans.add(span.copy(end = newEnd))
							}
						}
					} else {
						// Regular delete operation
						val newStart = operation.transformOffset(span.start, state)
						val newEnd = operation.transformOffset(span.end, state)
						if (newStart != newEnd) {
							updatedSpans.add(span.copy(start = newStart, end = newEnd))
						}
					}
				}

				is TextEditOperation.Replace -> {
					// Handle replace as a delete followed by insert
					val deleteOp = TextEditOperation.Delete(
						range = operation.range,
						deletedText = operation.oldText,
						cursorBefore = operation.cursorBefore,
						cursorAfter = operation.range.start
					)
					updateSpans(deleteOp)

					val insertOp = TextEditOperation.Insert(
						position = operation.range.start,
						text = operation.newText,
						cursorBefore = operation.range.start,
						cursorAfter = operation.cursorAfter
					)
					updateSpans(insertOp)
					return
				}
			}
		}

		spans.clear()
		spans.addAll(updatedSpans)
	}
}