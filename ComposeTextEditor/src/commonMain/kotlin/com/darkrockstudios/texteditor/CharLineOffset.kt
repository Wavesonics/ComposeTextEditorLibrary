package com.darkrockstudios.texteditor

import com.darkrockstudios.texteditor.state.TextEditorState

data class CharLineOffset(
	val line: Int,
	val char: Int,
) : Comparable<CharLineOffset> {
	override fun compareTo(other: CharLineOffset): Int {
		return when {
			line < other.line -> -1
			line > other.line -> 1
			else -> char.compareTo(other.char)
		}
	}

	infix fun isBefore(other: CharLineOffset): Boolean = this < other
	infix fun isAfter(other: CharLineOffset): Boolean = this > other
	infix fun isBeforeOrEqual(other: CharLineOffset): Boolean = this <= other
	infix fun isAfterOrEqual(other: CharLineOffset): Boolean = this >= other
}

fun CharLineOffset.toCharacterIndex(state: TextEditorState): Int {
	return state.getCharacterIndex(this)
}

fun Int.toCharLineOffset(state: TextEditorState): CharLineOffset {
	return state.getOffsetAtCharacter(this)
}
