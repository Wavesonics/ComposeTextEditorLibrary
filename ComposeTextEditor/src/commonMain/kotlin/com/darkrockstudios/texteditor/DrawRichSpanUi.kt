package com.darkrockstudios.texteditor

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.drawRichSpans(lineWrap: LineWrap, state: TextEditorState) {
	val textLayoutResult = lineWrap.textLayoutResult

	lineWrap.richSpans.forEach { richSpan ->
		// Get the range of text visible in this wrap
		val wrapVisibleStart = textLayoutResult.getLineStart(lineWrap.virtualLineIndex)
		val wrapVisibleEnd =
			textLayoutResult.getLineEnd(lineWrap.virtualLineIndex, visibleEnd = true)

		// Calculate where in the original line this wrapped segment starts and ends
		val lineStart = CharLineOffset(line = lineWrap.line, char = lineWrap.wrapStartsAtIndex)
		val lineEnd = CharLineOffset(
			line = lineWrap.line,
			char = lineWrap.wrapStartsAtIndex + (wrapVisibleEnd - wrapVisibleStart)
		)

		// Convert to absolute character indices
		val spanStartAbsChar = richSpan.range.start.toCharacterIndex(state)
		val spanEndAbsChar = richSpan.range.end.toCharacterIndex(state)
		val lineStartAbsChar = lineStart.toCharacterIndex(state)

		// If this wrapped segment intersects with our span
		if (spanStartAbsChar <= lineEnd.toCharacterIndex(state) &&
			spanEndAbsChar >= lineStart.toCharacterIndex(state)
		) {
			// Calculate position adjustment based on whether this is a wrapped line
			// Calculate the local range within this wrapped segment
			val localStart = if (spanStartAbsChar <= lineStartAbsChar) {
				wrapVisibleStart
			} else {
				(spanStartAbsChar - lineStartAbsChar) + wrapVisibleStart
			}

			val localEnd = if (spanEndAbsChar >= lineEnd.toCharacterIndex(state)) {
				wrapVisibleEnd
			} else {
				((spanEndAbsChar - lineStartAbsChar) + wrapVisibleStart)
					.coerceAtMost(wrapVisibleEnd)
			}

			val localRange = androidx.compose.ui.text.TextRange(
				start = localStart,
				end = localEnd
			)

			with(richSpan.style) {
				translate(top = lineWrap.offset.y - state.scrollState.value) {
					drawCustomStyle(
						layoutResult = textLayoutResult,
						lineWrap = lineWrap,
						textRange = localRange
					)
				}
			}
		}
	}
}