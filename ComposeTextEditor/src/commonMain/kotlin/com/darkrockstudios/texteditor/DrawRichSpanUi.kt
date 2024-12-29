package com.darkrockstudios.texteditor

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.drawRichSpans(lineWrap: LineWrap, state: TextEditorState) {
	val textLayoutResult = lineWrap.textLayoutResult
	// Determine if this is a wrapped line by checking if it's not the first virtual line
	val isWrappedLine = lineWrap.virtualLineIndex > 0

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
			val positionAdjustment = if (isWrappedLine) 1 else 0

			// Calculate the local range within this wrapped segment
			val localStart = if (spanStartAbsChar <= lineStartAbsChar) {
				wrapVisibleStart + positionAdjustment
			} else {
				(spanStartAbsChar - lineStartAbsChar) + wrapVisibleStart + positionAdjustment
			}

			val localEnd = if (spanEndAbsChar >= lineEnd.toCharacterIndex(state)) {
				wrapVisibleEnd + positionAdjustment
			} else {
				((spanEndAbsChar - lineStartAbsChar) + wrapVisibleStart + positionAdjustment)
					.coerceAtMost(wrapVisibleEnd + positionAdjustment)
			}

			val localRange = androidx.compose.ui.text.TextRange(
				start = localStart,
				end = localEnd
			)

			with(richSpan.style) {
				translate(top = lineWrap.offset.y) {
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