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
import com.darkrockstudios.texteditor.CodeFenceBoundary
import com.darkrockstudios.texteditor.LineWrap
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Decorative rich span that paints the "card" visuals for a markdown fenced code
 * block — a tinted background fill behind every line in the run plus a hairline
 * border that closes the top edge on the run's first line, the bottom edge on
 * the last, and the sides on every line. The actual monospace text style is
 * baked into the line's [androidx.compose.ui.text.SpanStyle] at apply time
 * (see [LineBlockStyle.textStyle]); this span is purely decorative.
 *
 * The visual indent is provided by [CODE_FENCE_PARAGRAPH_STYLE] so text doesn't
 * hug the left border. Multi-line scope: each line in the run carries its own
 * span, and [LineWrap.codeFenceBoundary] (filled in by `updateBookKeeping`)
 * tells the span which edges it owns.
 */
data object CodeFenceSpanStyle : RichSpanStyle {
	override val stickyAtStart: Boolean = true

	override fun DrawScope.drawCustomStyle(
		layoutResult: TextLayoutResult,
		lineWrap: LineWrap,
		textRange: TextRange,
		state: TextEditorState,
	) {
		val boundary = lineWrap.codeFenceBoundary ?: return
		val lineHeight = layoutResult.multiParagraph.getLineHeight(lineWrap.virtualLineIndex)

		val fillColor = if (state.codeFenceBackgroundColor.isSpecified) {
			state.codeFenceBackgroundColor
		} else {
			Color.Gray.copy(alpha = 0.18f)
		}
		val borderColor = if (state.codeFenceBorderColor.isSpecified) {
			state.codeFenceBorderColor
		} else {
			Color.Gray.copy(alpha = 0.55f)
		}
		val border = BORDER_WIDTH_DP.dp.toPx()

		// Background fill across the full editor width on every virtual line so
		// wrapped sub-lines also sit on the card.
		drawRect(
			color = fillColor,
			topLeft = Offset.Zero,
			size = Size(size.width, lineHeight),
		)

		// Side borders on every virtual line — these stack into one continuous
		// vertical edge across the run.
		drawRect(
			color = borderColor,
			topLeft = Offset.Zero,
			size = Size(border, lineHeight),
		)
		drawRect(
			color = borderColor,
			topLeft = Offset(size.width - border, 0f),
			size = Size(border, lineHeight),
		)

		// Top border closes the run on virtualLineIndex 0 of First/Only.
		val isTopBoundary = boundary == CodeFenceBoundary.First ||
				boundary == CodeFenceBoundary.Only
		if (isTopBoundary && lineWrap.virtualLineIndex == 0) {
			drawRect(
				color = borderColor,
				topLeft = Offset.Zero,
				size = Size(size.width, border),
			)
		}

		// Bottom border closes the run on the last virtual line of Last/Only.
		val isBottomBoundary = boundary == CodeFenceBoundary.Last ||
				boundary == CodeFenceBoundary.Only
		val isLastVirtual =
			lineWrap.virtualLineIndex == layoutResult.multiParagraph.lineCount - 1
		if (isBottomBoundary && isLastVirtual) {
			drawRect(
				color = borderColor,
				topLeft = Offset(0f, lineHeight - border),
				size = Size(size.width, border),
			)
		}
	}

	private const val BORDER_WIDTH_DP = 1f
}

/**
 * Visual indent applied to fenced code lines so the text breathes from the
 * left border. Same first-line and rest-line indent — code blocks don't have a
 * hanging indent.
 */
val CODE_FENCE_PARAGRAPH_STYLE: ParagraphStyle = ParagraphStyle(
	textIndent = TextIndent(firstLine = 12.sp, restLine = 12.sp),
)
