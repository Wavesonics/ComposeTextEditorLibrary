package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextRange
import com.darkrockstudios.texteditor.toCharLineOffset
import com.darkrockstudios.texteditor.toCharacterIndex

sealed class TextEditOperation {
	abstract val cursorBefore: CharLineOffset
	abstract val cursorAfter: CharLineOffset

	internal abstract fun transformOffset(
		offset: CharLineOffset,
		state: TextEditorState
	): CharLineOffset

	data class Insert(
		val position: CharLineOffset,
		val text: AnnotatedString,
		override val cursorBefore: CharLineOffset,
		override val cursorAfter: CharLineOffset
	) : TextEditOperation() {
		override fun transformOffset(
			offset: CharLineOffset,
			state: TextEditorState
		): CharLineOffset {
			// Edit happened after this Offset, not affected
			return if (offset.line < position.line ||
				(offset.line == position.line && offset.char < position.char)
			) {
				offset
			}
			// Earlier line
			else if (offset.line > position.line) {
				val lineShift = text.count { it == '\n' }
				offset.copy(line = offset.line + lineShift)
			}
			// Same line, and before the offset
			else if (offset.line == position.line && offset.char <= position.char) {
				val lineShift = text.count { it == '\n' }
				CharLineOffset(offset.line + lineShift, offset.char + text.length)
			} else {
				offset
			}
		}
	}

	data class Delete(
		val range: TextRange,
		val deletedText: AnnotatedString,
		override val cursorBefore: CharLineOffset,
		override val cursorAfter: CharLineOffset
	) : TextEditOperation() {
		override fun transformOffset(
			offset: CharLineOffset,
			state: TextEditorState
		): CharLineOffset {
			// Convert to absolute indices for easier math
			val absoluteOffset = offset.toCharacterIndex(state)
			// For Delete operations, we need to be careful about accessing the end position
			// as it might no longer exist in the document
			val deleteStart = range.start.toCharacterIndex(state)
			// Instead of accessing the end position directly, calculate it based on the deleted text
			val deleteLength = deletedText.length
			val deleteEnd = deleteStart + deleteLength

			return when {
				absoluteOffset < deleteStart -> offset
				absoluteOffset > deleteEnd ->
					(absoluteOffset - deleteLength).toCharLineOffset(state)

				else -> range.start
			}
		}
	}

	data class Replace(
		val range: TextRange,
		val newText: AnnotatedString,
		val oldText: AnnotatedString,
		override val cursorBefore: CharLineOffset,
		override val cursorAfter: CharLineOffset
	) : TextEditOperation() {
		override fun transformOffset(
			offset: CharLineOffset,
			state: TextEditorState
		): CharLineOffset {
			// Convert to absolute indices for easier math
			val absoluteOffset = offset.toCharacterIndex(state)
			val replaceStart = range.start.toCharacterIndex(state)
			// Similarly here, calculate the end based on the old text length
			val replaceLength = oldText.length
			val replaceEnd = replaceStart + replaceLength
			val lengthDelta = newText.length - replaceLength

			return when {
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

internal fun TextEditOperation.transformRange(range: TextRange, state: TextEditorState): TextRange {
	return TextRange(
		transformOffset(range.start, state),
		transformOffset(range.end, state)
	)
}
