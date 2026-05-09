package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.texteditor.CharLineOffset
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
		// Only draw the bullet on the first virtual sub-line of the paragraph; wrapped
		// sub-lines hang under the text without their own marker.
		if (lineWrap.virtualLineIndex != 0) return
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineWrap.virtualLineIndex)
		val centerX = BULLET_CENTER_DP.dp.toPx()
		val centerY = lineHeight / 2f
		drawCircle(
			color = Color.DarkGray,
			radius = BULLET_RADIUS_DP.dp.toPx(),
			center = Offset(centerX, centerY),
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

/**
 * Returns true if [line] currently has a [BulletListSpanStyle] rich span attached.
 */
internal fun TextEditorState.hasBulletList(line: Int): Boolean =
	richSpanManager.getAllRichSpans().any { span ->
		span.style === BulletListSpanStyle && span.range.start.line == line
	}

/**
 * Attaches bullet-list rendering to [line]: wraps the existing text in
 * [BULLET_LIST_PARAGRAPH_STYLE] and adds a [BulletListSpanStyle] rich span.
 *
 * Idempotent — no-op if the line already has a bullet span. Used both by toolbar
 * toggles and by the smart-Enter path that continues a list onto the next line.
 */
internal fun TextEditorState.applyBulletList(line: Int) {
	if (hasBulletList(line)) return
	val existing = textLines.getOrNull(line) ?: return
	val rebuilt = buildAnnotatedString {
		withStyle(BULLET_LIST_PARAGRAPH_STYLE) {
			append(existing)
		}
	}
	updateLine(line, rebuilt)
	addRichSpan(
		start = CharLineOffset(line, 0),
		end = CharLineOffset(line, existing.length.coerceAtLeast(1)),
		style = BulletListSpanStyle,
	)
}

/**
 * Removes bullet-list rendering from [line]: drops every [BulletListSpanStyle]
 * rich span anchored to it and rebuilds the line's [androidx.compose.ui.text.AnnotatedString]
 * without [BULLET_LIST_PARAGRAPH_STYLE] so the indent disappears.
 *
 * No-op if [line] is out of range or carries no bullet-list span.
 */
internal fun TextEditorState.demoteBulletList(line: Int) {
	val existing = textLines.getOrNull(line) ?: return
	val spans = richSpanManager.getAllRichSpans()
		.filter { it.style === BulletListSpanStyle && it.range.start.line == line }
	if (spans.isEmpty()) return
	spans.forEach { removeRichSpan(it) }
	val rebuilt = buildAnnotatedString {
		append(existing.text)
		existing.spanStyles.forEach { range ->
			addStyle(range.item, range.start, range.end)
		}
		existing.paragraphStyles.forEach { range ->
			if (range.item != BULLET_LIST_PARAGRAPH_STYLE) {
				addStyle(range.item, range.start, range.end)
			}
		}
	}
	updateLine(line, rebuilt)
}
