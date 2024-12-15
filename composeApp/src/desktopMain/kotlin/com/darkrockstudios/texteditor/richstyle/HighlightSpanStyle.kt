package com.darkrockstudios.texteditor.richstyle


import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

class HighlightSpanStyle(
	private val color: Color
) : RichSpanStyle {
	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		textRange: TextRange
	) {
		val lineIndex = layoutResult.getLineForOffset(textRange.start)
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineIndex)

		val startX = layoutResult.getHorizontalPosition(textRange.start, usePrimaryDirection = true)
		val endX = layoutResult.getHorizontalPosition(textRange.end, usePrimaryDirection = true)

		drawRect(
			color = color,
			topLeft = Offset(startX, 0f),
			size = Size(endX - startX, lineHeight)
		)
	}
}