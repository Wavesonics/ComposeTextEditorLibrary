package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * A line-anchored block style — bullet, blockquote, ordered-list item, or any
 * future gutter-marker style that pairs a [RichSpanStyle] decoration with a
 * [ParagraphStyle] indent and roundtrips through markdown via a single-line
 * prefix. Bundling these pieces makes adding a new style a one-instance change
 * instead of touching apply/demote/import/export/toggle/Enter/Backspace
 * separately.
 *
 * [markdownPattern] must capture the line body (after the marker) in group 1.
 *
 * [markdownPrefix] receives the 0-based position of the line within its
 * contiguous run of this block style — fixed-marker styles ignore it
 * (e.g. bullet always returns `"- "`); ordered lists use it to emit
 * `"${pos + 1}. "`.
 */
internal data class LineBlockStyle(
	val spanStyle: RichSpanStyle,
	val paragraphStyle: ParagraphStyle,
	val markdownPrefix: (positionInRun: Int) -> String,
	val markdownPattern: Regex,
)

internal val Blockquote = LineBlockStyle(
	spanStyle = BlockquoteSpanStyle,
	paragraphStyle = BLOCKQUOTE_PARAGRAPH_STYLE,
	markdownPrefix = { "> " },
	// Single-level only — nested `> > ` collapses one level per pass.
	markdownPattern = Regex("""^>\s?(.*)$"""),
)

internal val BulletList = LineBlockStyle(
	spanStyle = BulletListSpanStyle,
	paragraphStyle = BULLET_LIST_PARAGRAPH_STYLE,
	markdownPrefix = { "- " },
	// `-`, `*`, or `+` followed by at least one space. Nested (indented) bullets
	// are not yet supported.
	markdownPattern = Regex("""^[-*+]\s+(.*)$"""),
)

internal val OrderedList = LineBlockStyle(
	spanStyle = OrderedListSpanStyle,
	paragraphStyle = ORDERED_LIST_PARAGRAPH_STYLE,
	// Always emit incrementing numerals from 1 — markdown renderers normalise
	// any starting digit, but emitting `1. 2. 3.` matches what humans expect to
	// see in the source.
	markdownPrefix = { pos -> "${pos + 1}. " },
	// Any digit run followed by `.` and at least one space. Nested (indented)
	// lists aren't supported yet.
	markdownPattern = Regex("""^\d+\.\s+(.*)$"""),
)

/**
 * Registry of every known line-block style. Iterated by import/export, by the
 * editor's smart Enter/Backspace, and by toolbar toggles so that adding a new
 * line-anchored style only needs an entry here.
 *
 * Order matters at import time: the first matching pattern wins. OrderedList
 * comes before BulletList so a line like `1. item` isn't accidentally captured
 * by a bullet regex (it isn't currently — but the ordering is still defensive).
 */
internal val LINE_BLOCK_STYLES: List<LineBlockStyle> =
	listOf(Blockquote, OrderedList, BulletList)

/**
 * Line-block styles that are mutually exclusive — applying one demotes any
 * other in the set on the same line. Blockquote is intentionally NOT in this
 * set: a quoted bullet (`> - item`) or quoted ordered item (`> 1. item`) is
 * legitimate markdown and stacking the two is the right behavior.
 *
 * Without this, toggling ordered on a bullet line (or vice versa) would leave
 * both [ParagraphStyle] indents on the same range — which Compose rejects as
 * overlapping paragraph styles, blanking the line.
 */
internal val LIST_BLOCK_STYLES: Set<LineBlockStyle> = setOf(BulletList, OrderedList)

internal fun TextEditorState.hasLineBlock(line: Int, block: LineBlockStyle): Boolean =
	richSpanManager.getAllRichSpans().any { span ->
		span.style === block.spanStyle && span.range.start.line == line
	}

/**
 * Idempotent — no-op if [line] already carries [block]. Otherwise wraps the
 * line's text in the indent paragraph style and attaches a fresh rich span.
 *
 * On an empty line the span is zero-width `[0, 0)` — `RichSpan.intersectsWith`
 * special-cases sticky-at-start spans so the gutter marker still renders.
 * As soon as the user types a character, sticky-at-start keeps the span anchored
 * at column 0 while the end shifts forward, naturally tracking the line length.
 */
internal fun TextEditorState.applyLineBlock(line: Int, block: LineBlockStyle) {
	if (hasLineBlock(line, block)) return
	// Demote any conflicting list-type block before applying — otherwise the new
	// paragraph-style indent would overlap the old one and Compose blanks the
	// line on the next measure pass.
	if (block in LIST_BLOCK_STYLES) {
		LIST_BLOCK_STYLES
			.filter { it !== block && hasLineBlock(line, it) }
			.forEach { demoteLineBlock(line, it) }
	}
	val existing = textLines.getOrNull(line) ?: return
	val rebuilt = buildAnnotatedString {
		withStyle(block.paragraphStyle) {
			append(existing)
		}
	}
	updateLine(line, rebuilt)
	addRichSpan(
		start = CharLineOffset(line, 0),
		end = CharLineOffset(line, existing.length),
		style = block.spanStyle,
	)
}

/**
 * Drops every span anchored to [line] for [block] and rebuilds the line without
 * its indent paragraph style. No-op if [line] is out of range or has no such span.
 */
internal fun TextEditorState.demoteLineBlock(line: Int, block: LineBlockStyle) {
	val existing = textLines.getOrNull(line) ?: return
	val spans = richSpanManager.getAllRichSpans()
		.filter { it.style === block.spanStyle && it.range.start.line == line }
	if (spans.isEmpty()) return
	spans.forEach { removeRichSpan(it) }
	val rebuilt = buildAnnotatedString {
		append(existing.text)
		existing.spanStyles.forEach { range ->
			addStyle(range.item, range.start, range.end)
		}
		existing.paragraphStyles.forEach { range ->
			if (range.item != block.paragraphStyle) {
				addStyle(range.item, range.start, range.end)
			}
		}
	}
	updateLine(line, rebuilt)
}

/** Returns the [LineBlockStyle] currently attached to [line], or null if none. */
internal fun TextEditorState.detectLineBlock(line: Int): LineBlockStyle? =
	LINE_BLOCK_STYLES.firstOrNull { hasLineBlock(line, it) }
