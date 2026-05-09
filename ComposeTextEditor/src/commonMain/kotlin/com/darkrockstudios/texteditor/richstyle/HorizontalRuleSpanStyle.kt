package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.effectiveHeight
import com.darkrockstudios.texteditor.richstyle.HorizontalRuleSpanStyle.HEIGHT_DP

/**
 * A [BlockSpanStyle] that draws a horizontal rule across its line. Pair with
 * [HR_PLACEHOLDER] (a single space) on a line of its own — the editor sizes the
 * line to [HEIGHT_DP] (giving the rule breathing room above and below) and the
 * placeholder text is suppressed via [replacesText].
 *
 * Markdown roundtrip is handled by `MarkdownExtension`, which detects `---` /
 * `***` / `___` lines on import and re-emits `---` for any line carrying this
 * style on export.
 */
data object HorizontalRuleSpanStyle : BlockSpanStyle {

	override fun blockHeight(density: Density, viewportWidth: Float): Float =
		with(density) { HEIGHT_DP.dp.toPx() }

	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineWrap: LineWrap,
		textRange: TextRange,
	) {
		val height = lineWrap.blockHeight ?: lineWrap.effectiveHeight
		val midY = height / 2f
		drawLine(
			color = Color.Gray,
			start = Offset(x = 0f, y = midY),
			end = Offset(x = size.width, y = midY),
			strokeWidth = 1.5f,
			cap = Stroke.DefaultCap,
		)
	}

	private const val HEIGHT_DP = 24f
}

/** Single space character that anchors a [HorizontalRuleSpanStyle] to a line. */
const val HR_PLACEHOLDER: String = " "
