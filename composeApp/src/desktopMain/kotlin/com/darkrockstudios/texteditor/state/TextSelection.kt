package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.TextOffset

data class TextSelection(
	val start: TextOffset,
	val end: TextOffset
) {
	val isValid: Boolean get() = start != end

	fun isSingleLine(): Boolean = start.line == end.line
}

internal fun isBeforeInDocument(a: TextOffset, b: TextOffset): Boolean {
	return when {
		a.line < b.line -> true
		a.line > b.line -> false
		else -> a.char < b.char
	}
}