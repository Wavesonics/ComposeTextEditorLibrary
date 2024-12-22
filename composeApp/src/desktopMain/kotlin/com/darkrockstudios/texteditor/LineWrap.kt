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
)

fun LineWrap.wrapStartToCharacterIndex(state: TextEditorState): Int {
	return state.wrapStartToCharacterIndex(this)
}