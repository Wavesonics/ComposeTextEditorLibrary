package com.darkrockstudios.texteditor

import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawRichSpans(lineWrap: LineWrap) {
	val textLayoutResult = lineWrap.textLayoutResult
	val isWrappedLine = lineWrap.offset.y > 0

	println("\n\n------ Drawing RichSpans for LineWrap: ${lineWrap.line} ${lineWrap.wrapStartsAtIndex}")
	println("wrapStartsAtIndex: ${lineWrap.wrapStartsAtIndex}")
	println("vline Length: ${lineWrap.virtualLength}")
	println("vline index: ${lineWrap.virtualLineIndex}")
	println("isWrappedLine: $isWrappedLine")

	lineWrap.richSpans.forEach { richSpan ->
		println("------ Drawing ${richSpan.style.javaClass.simpleName}")
		println("Physical Span Start: ${richSpan.start}")
		println("Physical Span End: ${richSpan.end}")
		println("Line Offset: ${lineWrap.offset}")

		val localSpanRange = androidx.compose.ui.text.TextRange(
			start = richSpan.start.char.coerceAtLeast(lineWrap.wrapStartsAtIndex),
			end = richSpan.end.char.coerceAtMost(lineWrap.wrapStartsAtIndex + lineWrap.virtualLength)
		)

		with(richSpan.style) {
			drawCustomStyle(
				layoutResult = textLayoutResult,
				lineIndex = lineWrap.virtualLineIndex,
				textRange = localSpanRange
			)
		}
	}
}