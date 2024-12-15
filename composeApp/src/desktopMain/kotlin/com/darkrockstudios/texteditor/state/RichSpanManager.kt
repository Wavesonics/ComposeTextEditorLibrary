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
					val newStart = operation.transformOffset(span.start, state)
					val newEnd = operation.transformOffset(span.end, state)
					updatedSpans.add(span.copy(start = newStart, end = newEnd))
				}

				is TextEditOperation.Delete -> {
					// Transform the span's start and end positions
					val newStart = operation.transformOffset(span.start, state)
					val newEnd = operation.transformOffset(span.end, state)
					// Only add if the span still exists
					if (newStart != newEnd) {
						updatedSpans.add(span.copy(start = newStart, end = newEnd))
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
				is TextEditOperation.LineSplit -> {
					val newStart = operation.transformOffset(span.start, state)
					val newEnd = operation.transformOffset(span.end, state)
					updatedSpans.add(span.copy(start = newStart, end = newEnd))
				}
			}
		}

		spans.clear()
		spans.addAll(updatedSpans)
	}
}