package com.darkrockstudios.texteditor

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