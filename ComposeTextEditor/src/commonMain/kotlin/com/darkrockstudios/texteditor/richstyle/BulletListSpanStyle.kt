package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Decorative rich span that marks a line as a markdown bullet-list item — a small
 * filled dot is drawn in the indent gutter while the underlying text continues to
 * render normally (no [BlockSpanStyle.replacesText] semantics).
 *
 * The visual indent is provided by the [BULLET_LIST_PARAGRAPH_STYLE] applied to the
 * line at import time; this span only paints the dot on the first wrapped sub-line
 * (subsequent wraps hang under the text, not under the bullet).
 *
 * Single-line scope: each item in a multi-line bullet list carries its own span.
 * Nested lists are out of scope for now.
 */
data object BulletListSpanStyle : RichSpanStyle {
	override val stickyAtStart: Boolean = true

	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineWrap: LineWrap,
		textRange: TextRange,
	) {
		if (lineWrap.virtualLineIndex != 0) return
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineWrap.virtualLineIndex)
		drawCircle(
			color = Color.DarkGray,
			radius = BULLET_RADIUS_DP.dp.toPx(),
			center = Offset(BULLET_CENTER_DP.dp.toPx(), lineHeight / 2f),
		)
	}

	private const val BULLET_CENTER_DP = 8f
	private const val BULLET_RADIUS_DP = 2.5f
}

/**
 * Visual indent applied to bullet-list lines so the [BulletListSpanStyle] dot has
 * room before the text. Identical first-line and rest-line indents give a hanging
 * indent so wrapped lines align under the text rather than under the bullet.
 */
val BULLET_LIST_PARAGRAPH_STYLE: ParagraphStyle = ParagraphStyle(
	textIndent = TextIndent(firstLine = 16.sp, restLine = 16.sp),
)

internal fun TextEditorState.hasBulletList(line: Int): Boolean =
	hasLineBlockStyle(line, BulletListSpanStyle)

internal fun TextEditorState.applyBulletList(line: Int) =
	applyLineBlockStyle(line, BulletListSpanStyle, BULLET_LIST_PARAGRAPH_STYLE)

internal fun TextEditorState.demoteBulletList(line: Int) =
	demoteLineBlockStyle(line, BulletListSpanStyle, BULLET_LIST_PARAGRAPH_STYLE)
