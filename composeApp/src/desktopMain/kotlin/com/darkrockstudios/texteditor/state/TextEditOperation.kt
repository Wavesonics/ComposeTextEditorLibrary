package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextRange

sealed class TextEditOperation {
	abstract val cursorBefore: CharLineOffset
	abstract val cursorAfter: CharLineOffset

	data class Insert(
		val position: CharLineOffset,
		val text: AnnotatedString,
		override val cursorBefore: CharLineOffset,
		override val cursorAfter: CharLineOffset
	) : TextEditOperation()

	data class Delete(
		val range: TextRange,
		val deletedText: AnnotatedString,
		override val cursorBefore: CharLineOffset,
		override val cursorAfter: CharLineOffset
	) : TextEditOperation()

	data class Replace(
		val range: TextRange,
		val newText: AnnotatedString,
		val oldText: AnnotatedString,
		override val cursorBefore: CharLineOffset,
		override val cursorAfter: CharLineOffset
	) : TextEditOperation()
}

fun TextEditOperation.transformOffset(
	offset: CharLineOffset,
	state: TextEditorState
): CharLineOffset {
	// Convert to absolute indices for easier math
	val absoluteOffset = offset.toCharacterIndex(state)

	return when (this) {
		is TextEditOperation.Insert -> {
			val insertPoint = position.toCharacterIndex(state)
			when {
				absoluteOffset < insertPoint -> offset
				else -> (absoluteOffset + text.length).toCharLineOffset(state)
			}
		}

		is TextEditOperation.Delete -> {
			val deleteStart = range.start.toCharacterIndex(state)
			val deleteEnd = range.end.toCharacterIndex(state)
			when {
				absoluteOffset < deleteStart -> offset
				absoluteOffset > deleteEnd ->
					(absoluteOffset - (deleteEnd - deleteStart)).toCharLineOffset(state)

				else -> range.start
			}
		}

		is TextEditOperation.Replace -> {
			val replaceStart = range.start.toCharacterIndex(state)
			val replaceEnd = range.end.toCharacterIndex(state)
			val lengthDelta = newText.length - (replaceEnd - replaceStart)
			when {
				absoluteOffset < replaceStart -> offset
				absoluteOffset > replaceEnd ->
					(absoluteOffset + lengthDelta).toCharLineOffset(state)

				else -> {
					val relativePos = (absoluteOffset - replaceStart)
						.coerceAtMost(newText.length)
					(replaceStart + relativePos).toCharLineOffset(state)
				}
			}
		}
	}
}

fun TextEditOperation.transformRange(range: TextRange, state: TextEditorState): TextRange {
	return TextRange(
		transformOffset(range.start, state),
		transformOffset(range.end, state)
	)
}

// Helper extension functions for converting between CharLineOffset and character index
private fun CharLineOffset.toCharacterIndex(state: TextEditorState): Int {
	return state.getCharacterIndex(this)
}

private fun Int.toCharLineOffset(state: TextEditorState): CharLineOffset {
	return state.getOffsetAtCharacter(this)
}
