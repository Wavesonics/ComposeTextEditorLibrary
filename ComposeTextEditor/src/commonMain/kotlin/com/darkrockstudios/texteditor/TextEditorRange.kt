package com.darkrockstudios.texteditor

data class TextEditorRange(
	val start: CharLineOffset,
	val end: CharLineOffset
) {
	fun isSingleLine(): Boolean = start.line == end.line
	fun validate(): Boolean = end isAfter start
	fun containsLine(lineIndex: Int): Boolean {
		return lineIndex >= start.line && lineIndex <= end.line
	}

	companion object {
		fun fromOffsets(offset1: CharLineOffset, offset2: CharLineOffset): TextEditorRange {
			return if (offset1 isBefore offset2) {
				TextEditorRange(offset1, offset2)
			} else {
				TextEditorRange(offset2, offset1)
			}
		}
	}
}


fun CharLineOffset.toRange(end: CharLineOffset): TextEditorRange {
	return TextEditorRange.fromOffsets(this, end)
}