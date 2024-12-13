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
		// Since we're working with a single virtual line, we only need line 0
		val startX = layoutResult.getHorizontalPosition(textRange.start, usePrimaryDirection = true)
		val endX = layoutResult.getHorizontalPosition(textRange.end, usePrimaryDirection = true)

		val lineHeight = layoutResult.multiParagraph.getLineHeight(0)

		drawRect(
			color = color,
			topLeft = Offset(startX, 0f),  // y is 0 because we're translated to line position
			size = Size(endX - startX, lineHeight)
		)
	}
}
