package com.darkrockstudios.texteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import com.darkrockstudios.texteditor.state.TextEditorState

internal fun DrawScope.DrawPlaceholderText(
	state: TextEditorState,
	style: TextEditorStyle
) {
	drawText(
		textMeasurer = state.textMeasurer,
		text = style.placeholderText,
		style = state.textStyle.copy(
			color = style.placeholderColor,
		),
		topLeft = Offset(0f, 0f)
	)
}