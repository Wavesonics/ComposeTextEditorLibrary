package com.darkrockstudios.texteditor

import androidx.compose.ui.text.AnnotatedString
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

/** Returns `this` clamped into addressable positions in [textLines], or (0,0) if empty. */
internal fun CharLineOffset.coerceInto(textLines: List<AnnotatedString>): CharLineOffset {
	if (textLines.isEmpty()) return CharLineOffset(0, 0)
	val safeLine = line.coerceIn(0, textLines.lastIndex)
	val safeChar = char.coerceIn(0, textLines[safeLine].length)
	return if (safeLine == line && safeChar == char) this else CharLineOffset(safeLine, safeChar)
}

fun CharLineOffset.toCharacterIndex(state: TextEditorState): Int {
	return state.getCharacterIndex(this)
}

fun Int.toCharLineOffset(state: TextEditorState): CharLineOffset {
	return state.getOffsetAtCharacter(this)
}
