package com.darkrockstudios.texteditor

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.drawRichSpans(lineWrap: LineWrap, state: TextEditorState) {
	val textLayoutResult = lineWrap.textLayoutResult

	lineWrap.richSpans.forEach { richSpan ->
		// Get the range of text visible in this wrap
		val wrapVisibleStart = textLayoutResult.getLineStart(lineWrap.virtualLineIndex)
		val wrapVisibleEnd = textLayoutResult.getLineEnd(lineWrap.virtualLineIndex)

		// Calculate where in the original line this wrapped segment starts and ends
		val lineStart = CharLineOffset(line = lineWrap.line, char = lineWrap.wrapStartsAtIndex)
		val lineEnd = CharLineOffset(
			line = lineWrap.line,
			char = lineWrap.wrapStartsAtIndex + (wrapVisibleEnd - wrapVisibleStart)
		)

		// Convert to absolute character indices
		val spanStartAbsChar = richSpan.start.toCharacterIndex(state)
		val spanEndAbsChar = richSpan.end.toCharacterIndex(state)
		val lineStartAbsChar = lineStart.toCharacterIndex(state)

		// If this wrapped segment intersects with our span
		if (spanStartAbsChar <= lineEnd.toCharacterIndex(state) &&
			spanEndAbsChar >= lineStart.toCharacterIndex(state)
		) {

			// Calculate the local range within this wrapped segment
			val localStart = if (spanStartAbsChar <= lineStartAbsChar) {
				wrapVisibleStart
			} else {
				(spanStartAbsChar - lineStartAbsChar) + wrapVisibleStart
			}

			val localEnd = if (spanEndAbsChar >= lineEnd.toCharacterIndex(state)) {
				wrapVisibleEnd
			} else {
				((spanEndAbsChar - lineStartAbsChar) + wrapVisibleStart).coerceAtMost(wrapVisibleEnd)
			}

			// Ensure we include the last character by using a range that goes to the visible end
			val localRange = androidx.compose.ui.text.TextRange(
				start = localStart,
				end = localEnd
			)

			with(richSpan.style) {
				translate(top = lineWrap.offset.y) {
					drawCustomStyle(
						layoutResult = textLayoutResult,
						lineIndex = lineWrap.virtualLineIndex,
						textRange = localRange
					)
				}
			}
		}
	}
}