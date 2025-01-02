package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.TextEditorRange

data class WordSegment(
	val text: String,
	val range: TextEditorRange,
) {
	override fun toString(): String {
		return text + " (${range.start}-${range.end})"
	}
}