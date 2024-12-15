package com.darkrockstudios.texteditor

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate

internal fun DrawScope.drawRichSpans(lineWrap: LineWrap) {
	val textLayoutResult = lineWrap.textLayoutResult

	// Get the actual text range for this wrapped line segment
	val wrapStart = lineWrap.wrapStartsAtIndex
	val wrapEnd = if (textLayoutResult.lineCount > 0) {
		textLayoutResult.getLineEnd(0, visibleEnd = true)
	} else {
		wrapStart
	}

	lineWrap.richSpans.forEach { richSpan ->
		// Calculate the intersection of the span with this wrapped line segment
		val spanStart = when {
			richSpan.start.line == lineWrap.line -> maxOf(wrapStart, richSpan.start.char)
			else -> wrapStart
		}

		val spanEnd = when {
			richSpan.end.line == lineWrap.line -> minOf(wrapEnd, richSpan.end.char)
			else -> wrapEnd
		}

		if (spanStart < spanEnd) {
			// Create a text range relative to this wrapped segment
			val localSpanRange = androidx.compose.ui.text.TextRange(
				start = spanStart - wrapStart,
				end = spanEnd - wrapStart
			)

			// Apply the style with proper translation
			with(richSpan.style) {
				translate(lineWrap.offset.x, lineWrap.offset.y) {
					drawCustomStyle(
						layoutResult = textLayoutResult,
						textRange = localSpanRange
					)
				}
			}
		}
	}
}