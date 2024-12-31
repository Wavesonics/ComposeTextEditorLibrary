package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.richstyle.RichSpanStyle

class RichSpanManager(
	private val state: TextEditorState
) {
	private val spans = mutableSetOf<RichSpan>()

	fun getAllRichSpans(): Set<RichSpan> = spans

	fun addRichSpan(range: TextEditorRange, style: RichSpanStyle) {
		spans.add(RichSpan(range, style))
	}

	fun addRichSpan(start: CharLineOffset, end: CharLineOffset, style: RichSpanStyle) {
		spans.add(RichSpan(TextEditorRange(start, end), style))
	}

	fun removeRichSpan(start: CharLineOffset, end: CharLineOffset, style: RichSpanStyle) {
		spans.remove(RichSpan(TextEditorRange(start, end), style))
	}

	fun removeRichSpan(span: RichSpan) {
		spans.remove(span)
	}

	fun getSpansForLineWrap(lineWrap: LineWrap): List<RichSpan> {
		return spans.filter { it.intersectsWith(lineWrap) }
	}

	fun updateSpans(operation: TextEditOperation, metadata: OperationMetadata?) {
		val updatedSpans = mutableSetOf<RichSpan>()

		spans.forEach { span ->
			span.range.apply {
				when (operation) {
					is TextEditOperation.Insert -> handleInsert(operation, updatedSpans, span)
					is TextEditOperation.Delete -> handleDelete(
						metadata,
						operation,
						updatedSpans,
						span
					)

					is TextEditOperation.Replace -> {
						handleReplace(operation, metadata)
						return
					}

					is TextEditOperation.StyleSpan -> handleStyleSpan(updatedSpans, span)
				}
			}
		}

		spans.clear()
		spans.addAll(updatedSpans)
	}

	private fun TextEditorRange.handleInsert(
		operation: TextEditOperation.Insert,
		updatedSpans: MutableSet<RichSpan>,
		span: RichSpan
	) {
		if (operation.text.text == "\n") {
			// Special handling for newline insertion
			when {
				// Case 1: Newline inserted before the span
				operation.position.line < start.line ||
						(operation.position.line == start.line && operation.position.char <= start.char) -> {
					// Move entire span to new line
					val newStart = operation.transformOffset(start, state)
					val newEnd = operation.transformOffset(end, state)
					updatedSpans.add(
						span.copy(
							range = TextEditorRange(
								start = newStart,
								end = newEnd
							)
						)
					)
				}

				// Case 2: Newline inserted inside the span
				operation.position.line == start.line &&
						operation.position.char > start.char &&
						operation.position.char < end.char -> {
					// Calculate remaining length in original span after split point
					val remainingLength = end.char - operation.position.char

					// First part remains on original line
					updatedSpans.add(
						span.copy(
							range = span.range.copy(
								end = CharLineOffset(
									start.line,
									operation.position.char
								)
							)
						)
					)

					// Second part moves to new line
					// Only spans the remaining length from the original span
					updatedSpans.add(
						span.copy(
							span.range.copy(
								start = CharLineOffset(start.line + 1, 0),
								end = CharLineOffset(
									start.line + 1,
									remainingLength
								)
							)
						)
					)
				}

				// Case 3: Newline inserted after the span
				operation.position.line > start.line ||
						(operation.position.line == start.line && operation.position.char >= end.char) -> {
					// Keep span as is
					updatedSpans.add(span)
				}
			}
		} else {
			// Regular text insertion
			val newStart = operation.transformOffset(start, state)
			val newEnd = operation.transformOffset(end, state)
			updatedSpans.add(
				span.copy(
					range = span.range.copy(
						start = newStart,
						end = newEnd
					),
				)
			)
		}
	}

	private fun handleReplace(
		operation: TextEditOperation.Replace,
		metadata: OperationMetadata?
	) {
		// Handle replace as a delete followed by insert
		val deleteOp = TextEditOperation.Delete(
			range = operation.range,
			cursorBefore = operation.cursorBefore,
			cursorAfter = operation.range.start,
		)
		updateSpans(deleteOp, metadata)

		val insertOp = TextEditOperation.Insert(
			position = operation.range.start,
			text = operation.newText,
			cursorBefore = operation.range.start,
			cursorAfter = operation.cursorAfter
		)
		updateSpans(insertOp, metadata)
		return
	}

	private fun TextEditorRange.handleDelete(
		metadata: OperationMetadata?,
		operation: TextEditOperation.Delete,
		updatedSpans: MutableSet<RichSpan>,
		span: RichSpan
	) {
		if (metadata != null) {
			if (metadata.deletedText?.text == "\n") {
				// Special handling for newline deletion
				val deletionPoint = operation.range.start
				val nextLineStart = operation.range.end

				when {
					// Span is entirely before the deletion point on the first line
					end.line < deletionPoint.line ||
							(end.line == deletionPoint.line && end.char <= deletionPoint.char) -> {
						updatedSpans.add(span)
					}
					// Span is entirely on the second line
					start.line == nextLineStart.line -> {
						val newStart = CharLineOffset(
							deletionPoint.line,
							deletionPoint.char + start.char
						)
						val newEnd = CharLineOffset(
							deletionPoint.line,
							deletionPoint.char + end.char
						)
						updatedSpans.add(
							span.copy(
								range = TextEditorRange(
									start = newStart,
									end = newEnd
								)
							)
						)
					}
					// Span crosses the newline
					start.line == deletionPoint.line &&
							end.line == nextLineStart.line -> {
						val newEnd = CharLineOffset(
							deletionPoint.line,
							deletionPoint.char + end.char
						)

						updatedSpans.add(
							span.copy(
								range = span.range.copy(
									end = newEnd
								)
							)
						)
					}
				}
			} else {
				// Regular delete operation
				val newStart = operation.transformOffset(start, state)
				val newEnd = operation.transformOffset(end, state)
				if (newStart != newEnd) {
					updatedSpans.add(
						span.copy(
							range = TextEditorRange(
								start = newStart, end = newEnd
							)
						)
					)
				}
			}
		}
	}

	private fun handleStyleSpan(
		updatedSpans: MutableSet<RichSpan>,
		span: RichSpan
	) {
		// Noop for StyleSpan
		updatedSpans.add(span)
	}

	fun getSpansInRange(range: TextEditorRange): List<RichSpan> {
		return spans.filter { span ->
			span.range.start isBeforeOrEqual range.end && span.range.end isAfterOrEqual range.start
		}
	}
}