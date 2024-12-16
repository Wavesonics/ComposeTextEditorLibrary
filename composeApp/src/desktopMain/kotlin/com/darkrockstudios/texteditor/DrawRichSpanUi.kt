package com.darkrockstudios.texteditor

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.drawRichSpans(lineWrap: LineWrap, state: TextEditorState) {
	val textLayoutResult = lineWrap.textLayoutResult
	val isWrappedLine = lineWrap.offset.y > 0

//	println("\n\n------ Drawing RichSpans for LineWrap: ${lineWrap.line} ${lineWrap.wrapStartsAtIndex}")
//	println("wrapStartsAtIndex: ${lineWrap.wrapStartsAtIndex}")
//	println("vline Length: ${lineWrap.virtualLength}")
//	println("vline index: ${lineWrap.virtualLineIndex}")
//	println("isWrappedLine: $isWrappedLine")

	lineWrap.richSpans.forEach { richSpan ->
//		println("------ Drawing ${richSpan.style.javaClass.simpleName}")
//		println("Physical Span Start: ${richSpan.start}")
//		println("Physical Span End: ${richSpan.end}")
//		println("Line Offset: ${lineWrap.offset}")

		val lineStart = CharLineOffset(line = lineWrap.line, char = lineWrap.wrapStartsAtIndex)
		val lineEnd = CharLineOffset(
			line = lineWrap.line,
			char = lineWrap.wrapStartsAtIndex + lineWrap.virtualLength
		)

		val lineStartAbsChar = lineStart.toCharacterIndex(state)
		val lineEndAbsChar = lineEnd.toCharacterIndex(state)

		val spanStartAbsChar = richSpan.start.toCharacterIndex(state)
		val spanEndAbsChar = richSpan.end.toCharacterIndex(state)

		if (spanStartAbsChar <= lineEndAbsChar && spanEndAbsChar >= lineStartAbsChar) {
			// Truncate span to this line, and convert from global to local
			val localSpanRange = androidx.compose.ui.text.TextRange(
				start = spanStartAbsChar.coerceAtLeast(lineStartAbsChar) - lineStartAbsChar,
				end = spanEndAbsChar.coerceAtMost(lineEndAbsChar) - lineStartAbsChar
			)

			with(richSpan.style) {
				translate(top = lineWrap.offset.y) {
					drawCustomStyle(
						layoutResult = textLayoutResult,
						lineIndex = lineWrap.virtualLineIndex,
						textRange = localSpanRange
					)
				}
			}
		}
	}
}