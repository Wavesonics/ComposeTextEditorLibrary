package com.darkrockstudios.texteditor.codeeditor

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.BasicTextEditor
import com.darkrockstudios.texteditor.RichSpanClickListener
import com.darkrockstudios.texteditor.TextEditorStyle
import com.darkrockstudios.texteditor.focusBorder
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState
import kotlin.math.absoluteValue

private val GUTTER_START_PADDING = 8.dp
private val GUTTER_END_PADDING = 8.dp
private val GUTTER_END_MARGIN = 8.dp

private fun numDigits(number: Int): Int {
	if (number == 0) return 1

	var n = number.absoluteValue
	var digits = 0
	while (n > 0) {
		digits++
		n /= 10
	}

	return digits
}

private fun gutterWidth(state: TextEditorState, density: Density): Dp {
	val colWidth = with(density) {
		state.textMeasurer.measure("0").size.width.toDp()
	}
	val numLines = state.textLines.size

	val maxLineNumber = numLines.coerceAtLeast(1)
	val numDigits = maxOf(numDigits(maxLineNumber), 1)

	return GUTTER_START_PADDING + (colWidth * numDigits) + GUTTER_END_PADDING
}

private fun DrawScope.drawLineNumbers(
	line: Int,
	offset: Offset,
	state: TextEditorState,
	style: CodeEditorStyle,
	gutterWidth: Dp
) {
	val x = offset.x - (gutterWidth.toPx() - GUTTER_START_PADDING.toPx()) - GUTTER_END_MARGIN.toPx()
	val lineNumberOffset = offset.copy(x = x)
	val lineNumberText = (line + 1).toString()

	drawText(
		textMeasurer = state.textMeasurer,
		text = lineNumberText,
		style = TextStyle.Default.copy(
			color = style.gutterTextColor,
		),
		topLeft = lineNumberOffset
	)
}

@Composable
fun CodeEditor(
	state: TextEditorState = rememberTextEditorState(),
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	style: CodeEditorStyle = rememberCodeEditorStyle(),
	onRichSpanClick: RichSpanClickListener? = null,
) {
	val density = LocalDensity.current
	val gutterWidth by remember(state.lineOffsets) {
		derivedStateOf { gutterWidth(state, density) }
	}

	Surface(modifier = modifier.focusBorder(state.isFocused && enabled, style.baseStyle)) {
		BasicTextEditor(
			state = state,
			modifier = Modifier
				.padding(start = gutterWidth + GUTTER_END_MARGIN)
				.graphicsLayer { clip = false }
				.drawBehind {
					drawRect(
						color = style.gutterBackgroundColor,
						topLeft = Offset(-(gutterWidth.toPx() + GUTTER_END_MARGIN.toPx()), 0f),
						size = Size(gutterWidth.toPx(), size.height)
					)
				},
			enabled = enabled,
			style = style.baseStyle,
			onRichSpanClick = onRichSpanClick,
			decorateLine = { line: Int, offset: Offset, state: TextEditorState, _: TextEditorStyle ->
				drawLineNumbers(line, offset, state, style, gutterWidth)
			}
		)
	}
}
