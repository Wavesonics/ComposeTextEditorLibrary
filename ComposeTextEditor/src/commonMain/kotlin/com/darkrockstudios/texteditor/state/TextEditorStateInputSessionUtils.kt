package com.darkrockstudios.texteditor.state

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange


internal fun TextEditorState.textValueForLine(line: Int = cursor.position.line): TextFieldValue {
	val selection = selector.selection?.let { sel ->
		TextRange(start = sel.start.char, end = sel.end.char)
	} ?: TextRange(start = cursorPosition.char, end = cursorPosition.char)
	val value = TextFieldValue(
		annotatedString = textLines[line],
		selection = selection
	)
	return value
}

internal fun TextEditorState.handleTextEditCommand(command: EditCommand) {
	when (command) {
		is CommitTextCommand -> {
			val oldValue = textValueForLine()
			insertTypedString(command.text)
			inputSession?.updateState(oldValue = oldValue, newValue = textValueForLine())
		}

		is BackspaceCommand -> {
			val oldValue = textValueForLine()
			backspaceAtCursor()
			inputSession?.updateState(oldValue = oldValue, newValue = textValueForLine())
		}

		is DeleteAllCommand -> {
			val oldValue = textValueForLine()
			setText("")
			inputSession?.updateState(oldValue = oldValue, newValue = textValueForLine())
		}

		is DeleteSurroundingTextCommand -> {
			val oldValue = textValueForLine()
			handleDeleteSurrounding(command)
			inputSession?.updateState(oldValue = oldValue, newValue = textValueForLine())
		}

		is MoveCursorCommand -> {
			val oldValue = textValueForLine()
			if (command.amount > 0) {
				cursor.moveRight(command.amount)
				inputSession?.updateState(oldValue = oldValue, newValue = textValueForLine())
			} else if (command.amount < 0) {
				cursor.moveLeft(command.amount)
				inputSession?.updateState(oldValue = oldValue, newValue = textValueForLine())
			}
		}
	}
}

private fun TextEditorState.handleDeleteSurrounding(command: DeleteSurroundingTextCommand) {
	// Delete text before the cursor
	if (command.lengthBeforeCursor > 0) {
		val startChar = maxOf(0, cursorPosition.char - command.lengthBeforeCursor)
		val beforeRange = TextEditorRange(
			start = CharLineOffset(cursorPosition.line, startChar),
			end = cursorPosition
		)
		delete(beforeRange)
	}

	// Delete text after the cursor
	if (command.lengthAfterCursor > 0) {
		val endChar = minOf(textLines[cursorPosition.line].length, cursorPosition.char + command.lengthAfterCursor)
		val afterRange = TextEditorRange(
			start = cursorPosition,
			end = CharLineOffset(cursorPosition.line, endChar)
		)
		delete(afterRange)
	}
}