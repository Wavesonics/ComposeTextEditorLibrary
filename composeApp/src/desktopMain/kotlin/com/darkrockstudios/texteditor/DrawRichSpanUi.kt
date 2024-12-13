package com.darkrockstudios.texteditor

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate

internal fun DrawScope.drawRichSpans(lineWrap: LineWrap) {
	val textLayoutResult = lineWrap.textLayoutResult
	val lineStart = lineWrap.wrapStartsAtIndex
	val lineEnd = if (textLayoutResult.lineCount > 0) {
		textLayoutResult.getLineEnd(0, visibleEnd = true)
	} else {
		lineStart
	}

	lineWrap.richSpans.forEach { richSpan ->
		val spanStart = when {
			richSpan.start.line == lineWrap.line -> maxOf(lineStart, richSpan.start.char)
			else -> lineStart
		}

		val spanEnd = when {
			richSpan.end.line == lineWrap.line -> minOf(lineEnd, richSpan.end.char)
			else -> lineEnd
		}

		if (spanStart < spanEnd) {
			val localSpanRange = androidx.compose.ui.text.TextRange(
				start = spanStart - lineStart,
				end = spanEnd - lineStart
			)

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