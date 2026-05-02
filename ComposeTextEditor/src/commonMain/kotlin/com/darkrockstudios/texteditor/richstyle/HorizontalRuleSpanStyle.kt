package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.darkrockstudios.texteditor.LineWrap

/**
 * Custom rich-span style that draws a horizontal rule across the line it covers.
 *
 * Pair with the placeholder space character ([HR_PLACEHOLDER]) — the editor needs at least
 * one character on the line for the span to anchor to. The line should contain only the
 * placeholder; once the user types on the line the rule should be removed (see
 * `MarkdownExtension`'s import/export which handle the markdown-side roundtrip via `---`).
 */
data object HorizontalRuleSpanStyle : RichSpanStyle {
	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineWrap: LineWrap,
		textRange: TextRange,
	) {
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineWrap.virtualLineIndex)
		val left = layoutResult.getLineLeft(lineWrap.virtualLineIndex)
		val right = layoutResult.getLineRight(lineWrap.virtualLineIndex)
		val end = if (right > left) right else size.width
		val midY = lineHeight / 2f
		drawLine(
			color = Color.Gray,
			start = Offset(x = left, y = midY),
			end = Offset(x = end, y = midY),
			strokeWidth = 1.5f,
			cap = Stroke.DefaultCap,
		)
	}
}

/** Single space character that anchors a [HorizontalRuleSpanStyle] to a line. */
const val HR_PLACEHOLDER: String = " "
