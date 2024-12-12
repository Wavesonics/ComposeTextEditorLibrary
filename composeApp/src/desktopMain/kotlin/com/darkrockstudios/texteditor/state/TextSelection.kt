package com.darkrockstudios.texteditor.state

import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextRange
import com.darkrockstudios.texteditor.toRange

data class TextSelection(
	val start: CharLineOffset,
	val end: CharLineOffset
) {
	val range: TextRange
		get() = start.toRange(end)

	val isValid: Boolean get() = start != end && start isBefore end

	fun isSingleLine(): Boolean = start.line == end.line
}