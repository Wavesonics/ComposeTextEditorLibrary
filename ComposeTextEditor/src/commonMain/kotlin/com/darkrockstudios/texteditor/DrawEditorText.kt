package com.darkrockstudios.texteditor

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.util.fastForEach
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.DrawEditorText(
	state: TextEditorState,
	style: TextEditorStyle
) {
	var lastLine = -1
	state.lineOffsets.fastForEach { virtualLine ->
		if (lastLine != virtualLine.line && state.textLines.size > virtualLine.line) {
			val line = state.textLines[virtualLine.line]

			drawText(
				textMeasurer = state.textMeasurer,
				text = line,
				topLeft = virtualLine.offset,
				style = TextStyle.Default.copy(
					color = style.placeholderColor,
				)
			)

			lastLine = virtualLine.line
		}

		drawRichSpans(virtualLine, state)
	}
}