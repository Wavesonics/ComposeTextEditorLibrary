package com.darkrockstudios.texteditor

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState

internal val GUTTER_START_PADDING = 4.dp
internal val GUTTER_WIDTH = 24.dp
internal val GUTTER_PADDING = 8.dp

fun DrawScope.drawLineNumbers(
	line: Int,
	offset: Offset,
	state: TextEditorState,
	style: TextEditorStyle,
) {
	val x = offset.x - GUTTER_WIDTH.toPx() - GUTTER_PADDING.toPx()
	val lineNumberOffset = offset.copy(x = x + GUTTER_START_PADDING.toPx())
	val lineNumberText = (line + 1).toString()

	val textLayoutResult = state.lineOffsets[line].textLayoutResult
	val lineHeight = textLayoutResult.size.height.toFloat()

	drawRect(
		color = style.unfocusedBorderColor,
		topLeft = Offset(x, offset.y),
		size = Size(GUTTER_WIDTH.toPx(), lineHeight)
	)

	drawText(
		textMeasurer = state.textMeasurer,
		text = lineNumberText,
		style = TextStyle.Default.copy(
			color = style.placeholderColor,
		),
		topLeft = lineNumberOffset
	)
}

@Composable
fun CodeEditor(
	state: TextEditorState = rememberTextEditorState(),
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	style: TextEditorStyle = rememberTextEditorStyle(),
	onRichSpanClick: RichSpanClickListener? = null,
) {
	Surface(modifier = modifier.focusBorder(state.isFocused && enabled, style)) {
		BasicTextEditor(
			state = state,
			modifier = Modifier.padding(start = GUTTER_WIDTH + GUTTER_PADDING).graphicsLayer { clip = false },
			enabled = enabled,
			style = style,
			onRichSpanClick = onRichSpanClick,
			decorateLine = { line: Int, offset: Offset, state: TextEditorState, style: TextEditorStyle ->
				drawLineNumbers(line, offset, state, style)
			}
		)
	}
}
