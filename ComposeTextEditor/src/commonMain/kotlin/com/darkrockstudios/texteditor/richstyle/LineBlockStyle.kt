package com.darkrockstudios.texteditor.richstyle

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.state.TextEditorState

/**
 * Shared apply / demote / has helpers for line-anchored block styles — bullet lists,
 * blockquotes, and any future single-line gutter marker that pairs a [RichSpanStyle]
 * with a [ParagraphStyle] indent. Each style exposes a typed wrapper (e.g.
 * [TextEditorState.applyBulletList]) that forwards to the helper here.
 */

internal fun TextEditorState.hasLineBlockStyle(line: Int, spanStyle: RichSpanStyle): Boolean =
	richSpanManager.getAllRichSpans().any { span ->
		span.style === spanStyle && span.range.start.line == line
	}

/**
 * Idempotent — no-op if [line] already carries [spanStyle]. Otherwise wraps the
 * line's text in [paragraphStyle] (giving room for the gutter marker) and attaches
 * a fresh [spanStyle] rich span.
 */
internal fun TextEditorState.applyLineBlockStyle(
	line: Int,
	spanStyle: RichSpanStyle,
	paragraphStyle: ParagraphStyle,
) {
	if (hasLineBlockStyle(line, spanStyle)) return
	val existing = textLines.getOrNull(line) ?: return
	val rebuilt = buildAnnotatedString {
		withStyle(paragraphStyle) {
			append(existing)
		}
	}
	updateLine(line, rebuilt)
	addRichSpan(
		start = CharLineOffset(line, 0),
		end = CharLineOffset(line, existing.length.coerceAtLeast(1)),
		style = spanStyle,
	)
}

/**
 * Drops every [spanStyle] rich span anchored to [line] and rebuilds the line's
 * AnnotatedString without [paragraphStyle], so both the gutter marker and the
 * indent disappear. No-op if [line] is out of range or carries no such span.
 */
internal fun TextEditorState.demoteLineBlockStyle(
	line: Int,
	spanStyle: RichSpanStyle,
	paragraphStyle: ParagraphStyle,
) {
	val existing = textLines.getOrNull(line) ?: return
	val spans = richSpanManager.getAllRichSpans()
		.filter { it.style === spanStyle && it.range.start.line == line }
	if (spans.isEmpty()) return
	spans.forEach { removeRichSpan(it) }
	val rebuilt = buildAnnotatedString {
		append(existing.text)
		existing.spanStyles.forEach { range ->
			addStyle(range.item, range.start, range.end)
		}
		existing.paragraphStyles.forEach { range ->
			if (range.item != paragraphStyle) {
				addStyle(range.item, range.start, range.end)
			}
		}
	}
	updateLine(line, rebuilt)
}
