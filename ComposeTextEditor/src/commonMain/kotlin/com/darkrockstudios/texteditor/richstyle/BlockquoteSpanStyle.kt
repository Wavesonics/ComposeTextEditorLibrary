package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
	) {
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineWrap.virtualLineIndex)
		val barLeft = BAR_LEFT_DP.dp.toPx()
		val barWidth = BAR_WIDTH_DP.dp.toPx()
		drawRect(
			color = Color.Gray.copy(alpha = 0.6f),
			topLeft = Offset(barLeft, 0f),
			size = Size(barWidth, lineHeight),
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

/**
 * Returns true if [line] currently has a [BlockquoteSpanStyle] rich span attached.
 */
internal fun TextEditorState.hasBlockquote(line: Int): Boolean =
	richSpanManager.getAllRichSpans().any { span ->
		span.style === BlockquoteSpanStyle && span.range.start.line == line
	}

/**
 * Attaches blockquote rendering to [line]: wraps the existing text in
 * [BLOCKQUOTE_PARAGRAPH_STYLE] and adds a [BlockquoteSpanStyle] rich span.
 *
 * Idempotent — no-op if the line already has a blockquote span. Used both by
 * toolbar toggles and by the smart-Enter path that continues a quote onto the
 * next line.
 */
internal fun TextEditorState.applyBlockquote(line: Int) {
	if (hasBlockquote(line)) return
	val existing = textLines.getOrNull(line) ?: return
	val rebuilt = buildAnnotatedString {
		withStyle(BLOCKQUOTE_PARAGRAPH_STYLE) {
			append(existing)
		}
	}
	updateLine(line, rebuilt)
	addRichSpan(
		start = CharLineOffset(line, 0),
		end = CharLineOffset(line, existing.length.coerceAtLeast(1)),
		style = BlockquoteSpanStyle,
	)
}

/**
 * Removes blockquote rendering from [line]: drops every [BlockquoteSpanStyle]
 * rich span anchored to it and rebuilds the line's [androidx.compose.ui.text.AnnotatedString]
 * without [BLOCKQUOTE_PARAGRAPH_STYLE] so the indent disappears.
 *
 * No-op if [line] is out of range or carries no blockquote span.
 */
internal fun TextEditorState.demoteBlockquote(line: Int) {
	val existing = textLines.getOrNull(line) ?: return
	val spans = richSpanManager.getAllRichSpans()
		.filter { it.style === BlockquoteSpanStyle && it.range.start.line == line }
	if (spans.isEmpty()) return
	spans.forEach { removeRichSpan(it) }
	val rebuilt = buildAnnotatedString {
		append(existing.text)
		existing.spanStyles.forEach { range ->
			addStyle(range.item, range.start, range.end)
		}
		existing.paragraphStyles.forEach { range ->
			if (range.item != BLOCKQUOTE_PARAGRAPH_STYLE) {
				addStyle(range.item, range.start, range.end)
			}
		}
	}
	updateLine(line, rebuilt)
}
