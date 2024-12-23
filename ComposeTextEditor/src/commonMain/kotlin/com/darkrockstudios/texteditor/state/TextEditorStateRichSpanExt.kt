package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.richstyle.RichSpan

/**
 * Extension functions for TextEditorState to inspect spans at a given position
 */

fun TextEditorState.getRichSpansAtPosition(position: CharLineOffset): List<RichSpan> {
	// Get the line wrap that contains our position
	val lineWrap = lineOffsets.firstOrNull { wrap ->
		wrap.line == position.line && position.char >= wrap.wrapStartsAtIndex
	} ?: return emptyList()

	// Filter spans that contain the position
	return lineWrap.richSpans.filter { span ->
		span.containsPosition(position)
	}
}
