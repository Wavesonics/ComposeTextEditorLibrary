package com.darkrockstudios.texteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.state.TextEditorState

data class LineWrap(
	val line: Int,
	// The character index of the first character on this line
	val wrapStartsAtIndex: Int,
	val virtualLength: Int,
	val virtualLineIndex: Int,
	val offset: Offset,
	val textLayoutResult: TextLayoutResult,
	val richSpans: List<RichSpan> = emptyList(),
	/**
	 * Y position of virtualLineIndex 0 of this paragraph — the same value across
	 * every virtual sub-line of one paragraph. Used by drawing to anchor
	 * `drawText` (which paints the whole textLayoutResult) at the paragraph's
	 * top even when the first wrap is culled above the viewport.
	 */
	val paragraphTop: Float = offset.y,
	/**
	 * If non-null, overrides the line's visual height. Used by [BlockSpanStyle]
	 * spans (e.g. block images) to occupy more vertical space than the underlying
	 * placeholder text would.
	 */
	val blockHeight: Float? = null,
)

val LineWrap.effectiveHeight: Float
	get() = blockHeight
		?: textLayoutResult.multiParagraph.getLineHeight(virtualLineIndex)

fun LineWrap.wrapStartToCharacterIndex(state: TextEditorState): Int {
	return state.wrapStartToCharacterIndex(this)
}