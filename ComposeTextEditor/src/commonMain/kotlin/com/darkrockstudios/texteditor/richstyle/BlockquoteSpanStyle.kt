package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Decorative rich span that marks a line as a markdown blockquote — a left bar
 * is drawn in the indent gutter while the underlying text continues to render
 * normally (no [BlockSpanStyle.replacesText] semantics).
 *
 * The visual indent is provided by the [BLOCKQUOTE_PARAGRAPH_STYLE] applied to
 * the line at import time; this span only paints the bar over each wrapped sub-line.
 *
 * Single-line scope: each line in a multi-line markdown blockquote carries its
 * own [BlockquoteSpanStyle] span. The bar segments stack visually to form a
 * continuous bar across the quoted block.
 */
data object BlockquoteSpanStyle : RichSpanStyle {
	override val stickyAtStart: Boolean = true

	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineWrap: LineWrap,
		textRange: TextRange,
		state: TextEditorState,
	) {
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineWrap.virtualLineIndex)
		val color = if (state.blockquoteBarColor.isSpecified) {
			state.blockquoteBarColor
		} else {
			Color.Gray.copy(alpha = 0.6f)
		}
		drawRect(
			color = color,
			topLeft = Offset(BAR_LEFT_DP.dp.toPx(), 0f),
			size = Size(BAR_WIDTH_DP.dp.toPx(), lineHeight),
		)
	}

	private const val BAR_LEFT_DP = 4f
	private const val BAR_WIDTH_DP = 3f
}

/**
 * Visual indent applied to blockquote lines so the [BlockquoteSpanStyle] bar has
 * room before the text. Also used by export to know which lines were blockquotes
 * (via the attached [BlockquoteSpanStyle] rich span — the paragraph style itself
 * is just a layout hint).
 */
val BLOCKQUOTE_PARAGRAPH_STYLE: ParagraphStyle = ParagraphStyle(
	textIndent = TextIndent(firstLine = 16.sp, restLine = 16.sp),
)
