package com.darkrockstudios.texteditor

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.moveCursorDown
import com.darkrockstudios.texteditor.state.moveCursorLeft
import com.darkrockstudios.texteditor.state.moveCursorRight
import com.darkrockstudios.texteditor.state.moveCursorToLineEnd
import com.darkrockstudios.texteditor.state.moveCursorToLineStart
import com.darkrockstudios.texteditor.state.moveCursorUp

internal fun Modifier.textEditorKeyboardInputHandler(
	state: TextEditorState,
	clipboardManager: ClipboardManager
): Modifier {
	return this.onPreviewKeyEvent { keyEvent ->
		if (keyEvent.type == KeyEventType.KeyDown) {
			when {
				keyEvent.isCtrlPressed -> handleShortcutKey(keyEvent, state, clipboardManager)
				else -> handleEditorKey(keyEvent, state)
			}
		} else {
			false
		}
	}
}

private fun handleShortcutKey(
	keyEvent: KeyEvent,
	state: TextEditorState,
	clipboardManager: ClipboardManager
): Boolean {
	return when (keyEvent.key) {
		Key.A -> {
			state.selector.selectAll()
			true
		}
		Key.C -> {
			state.selector.selection?.let {
				val selectedText = state.selector.getSelectedText()
				clipboardManager.setText(AnnotatedString(selectedText))
			}
			true
		}

		Key.X -> {
			state.selector.selection?.let {
				val selectedText = state.selector.getSelectedText()
				state.selector.deleteSelection()
				clipboardManager.setText(AnnotatedString(selectedText))
			}
			true
		}

		Key.V -> {
			clipboardManager.getText()?.text?.let { text ->
				val curSelection = state.selector.selection
				if (curSelection != null) {
					state.replace(curSelection.range, text)
				} else {
					state.insertStringAtCursor(text)
				}
			}
			state.selector.clearSelection()
			true
		}
		else -> false
	}
}

private fun handleEditorKey(keyEvent: KeyEvent, state: TextEditorState): Boolean {
	return when (keyEvent.key) {
		Key.Delete -> {
			if (state.selector.selection != null) {
				state.selector.deleteSelection()
			} else {
				state.deleteAtCursor()
			}
			true
		}

		Key.Backspace -> {
			if (state.selector.selection != null) {
				state.selector.deleteSelection()
			} else {
				state.backspaceAtCursor()
			}
			true
		}

		Key.Enter -> {
			if (state.selector.selection != null) {
				state.selector.deleteSelection()
			}
			state.insertNewlineAtCursor()
			true
		}

		Key.DirectionLeft -> {
			if (keyEvent.isShiftPressed) {
				val initialPosition = state.cursorPosition
				state.moveCursorLeft()
				updateSelectionForCursorMovement(state, initialPosition)
			} else {
				state.selector.clearSelection()
				state.moveCursorLeft()
			}
			true
		}

		Key.DirectionRight -> {
			if (keyEvent.isShiftPressed) {
				val initialPosition = state.cursorPosition
				state.moveCursorRight()
				updateSelectionForCursorMovement(state, initialPosition)
			} else {
				state.selector.clearSelection()
				state.moveCursorRight()
			}
			true
		}

		Key.DirectionUp -> {
			if (keyEvent.isShiftPressed) {
				val initialPosition = state.cursorPosition
				state.moveCursorUp()
				updateSelectionForCursorMovement(state, initialPosition)
			} else {
				state.selector.clearSelection()
				state.moveCursorUp()
			}
			true
		}

		Key.DirectionDown -> {
			if (keyEvent.isShiftPressed) {
				val initialPosition = state.cursorPosition
				state.moveCursorDown()
				updateSelectionForCursorMovement(state, initialPosition)
			} else {
				state.selector.clearSelection()
				state.moveCursorDown()
			}
			true
		}

		Key.MoveHome -> {
			if (keyEvent.isShiftPressed) {
				val initialPosition = state.cursorPosition
				state.moveCursorToLineStart()
				updateSelectionForCursorMovement(state, initialPosition)
			} else {
				state.selector.clearSelection()
				state.moveCursorToLineStart()
			}
			true
		}

		Key.MoveEnd -> {
			if (keyEvent.isShiftPressed) {
				val initialPosition = state.cursorPosition
				state.moveCursorToLineEnd()
				updateSelectionForCursorMovement(state, initialPosition)
			} else {
				state.selector.clearSelection()
				state.moveCursorToLineEnd()
			}
			true
		}

		else -> {
			val char = keyEventToUTF8Character(keyEvent)
			if (char != null) {
				if (state.selector.selection != null) {
					state.selector.deleteSelection()
				}
				state.insertCharacterAtCursor(char)
				true
			} else {
				false
			}
		}
	}
}

private fun isVisibleCharacter(char: Char): Boolean {
	return char.isLetterOrDigit() || char.isWhitespace() || char in "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
}

private fun keyEventToUTF8Character(keyEvent: KeyEvent): Char? {
	return if (keyEvent.type == KeyEventType.KeyDown) {
		val keyCode = keyEvent.utf16CodePoint
		if (keyCode in Char.MIN_VALUE.code..Char.MAX_VALUE.code) {
			val newChar = keyCode.toChar()
			if (isVisibleCharacter(newChar)) {
				newChar
			} else {
				null
			}
		} else {
			null
		}
	} else {
		null
	}
}

private fun updateSelectionForCursorMovement(
	state: TextEditorState,
	initialPosition: CharLineOffset
) {
	val currentSelection = state.selector.selection
	when {
		// No existing selection - start a new one
		currentSelection == null -> {
			state.selector.updateSelection(initialPosition, state.cursorPosition)
		}
		// Extend/modify existing selection
		else -> {
			// If we were at the start of the selection, keep the end fixed
			if (initialPosition == currentSelection.start) {
				state.selector.updateSelection(state.cursorPosition, currentSelection.end)
			}
			// If we were at the end of the selection, keep the start fixed
			else if (initialPosition == currentSelection.end) {
				state.selector.updateSelection(currentSelection.start, state.cursorPosition)
			}
			// If cursor was outside selection, create new selection
			else {
				state.selector.updateSelection(initialPosition, state.cursorPosition)
			}
		}
	}
}