package com.darkrockstudios.texteditor

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.util.fastForEach
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.DrawEditorText(
	state: TextEditorState,
	style: TextEditorStyle,
	decorateLine: LineDecorator?,
) {
	// Get current scroll position and viewport height
	val scrollY = state.scrollState.value
	val viewportHeight = size.height

	// Calculate visible range with some padding to ensure smooth scrolling
	val minY = (scrollY - viewportHeight * 0.1f).coerceAtLeast(0f)
	val maxY = scrollY + viewportHeight

	var lastLine = -1
	state.lineOffsets.fastForEach { virtualLine ->
		// Check if this line could be visible
		val lineTop = virtualLine.offset.y
		val lineHeight = virtualLine.textLayoutResult.size.height
		val lineBottom = lineTop + lineHeight

		if (lineBottom >= minY && lineTop <= maxY) {
			if (lastLine != virtualLine.line && state.textLines.size > virtualLine.line) {
				val line = state.textLines[virtualLine.line]

				val offset = virtualLine.offset.copy(y = virtualLine.offset.y - scrollY)
				decorateLine?.let {
					decorateLine(virtualLine.line, offset, state, style)
				}

				drawText(
					textMeasurer = state.textMeasurer,
					text = line,
					topLeft = offset,
					style = TextStyle.Default.copy(
						color = style.textColor,
					)
				)

				lastLine = virtualLine.line
			}

			drawRichSpans(virtualLine, state)
		}
	}
}