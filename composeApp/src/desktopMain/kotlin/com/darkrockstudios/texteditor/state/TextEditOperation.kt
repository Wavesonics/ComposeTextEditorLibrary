package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextRange

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
			// Before insertion point - no change
			if (offset.line < position.line ||
				(offset.line == position.line && offset.char < position.char)
			) {
				return offset
			}

			// After insertion on later lines
			if (offset.line > position.line) {
				val lineShift = text.count { it == '\n' }
				return offset.copy(line = offset.line + lineShift)
			}

			// On same line after insertion point
			if (offset.line == position.line && offset.char >= position.char) {
				if (text.contains('\n')) {
					val lineShift = text.count { it == '\n' }
					val lastLineLength = text.text.substringAfterLast('\n').length
					return CharLineOffset(
						offset.line + lineShift,
						offset.char - position.char + lastLineLength
					)
				} else {
					return offset.copy(char = offset.char + text.length)
				}
			}

			return offset
		}
	}

	data class Delete(
		val range: TextRange,
		override val cursorBefore: CharLineOffset,
		override val cursorAfter: CharLineOffset
	) : TextEditOperation() {
		override fun transformOffset(
			offset: CharLineOffset,
			state: TextEditorState
		): CharLineOffset {
			// If before the deletion range entirely
			if (offset.line < range.start.line) {
				return offset
			}

			// If after the deletion range entirely, adjust line number
			if (offset.line > range.end.line) {
				val linesDelta = range.end.line - range.start.line
				return offset.copy(line = offset.line - linesDelta)
			}

			// If on the start line but before deletion point
			if (offset.line == range.start.line && offset.char < range.start.char) {
				return offset
			}

			// If on the same line after deletion point
			if (offset.line == range.start.line && offset.char > range.end.char) {
				val charDelta = range.end.char - range.start.char
				return offset.copy(char = offset.char - charDelta)
			}

			// If within the deletion range
			if (offset.line >= range.start.line && offset.line <= range.end.line) {
				return range.start
			}

			return offset
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
			// If offset is on a different line, keep it unchanged
			if (offset.line < range.start.line || offset.line > range.end.line) {
				return offset
			}

			// If on start line but before replacement
			if (offset.line == range.start.line && offset.char < range.start.char) {
				return offset
			}

			// If on end line but after replacement
			if (offset.line == range.end.line && offset.char > range.end.char) {
				val lengthDelta = newText.length - oldText.length
				return offset.copy(char = offset.char + lengthDelta)
			}

			// If within the replacement range
			if (offset.line == range.start.line) {
				val relativePos = offset.char - range.start.char
				return offset.copy(char = range.start.char + relativePos)
			}

			return offset
		}
	}
}

internal fun TextEditOperation.transformRange(range: TextRange, state: TextEditorState): TextRange {
	return TextRange(
		transformOffset(range.start, state),
		transformOffset(range.end, state)
	)
}
