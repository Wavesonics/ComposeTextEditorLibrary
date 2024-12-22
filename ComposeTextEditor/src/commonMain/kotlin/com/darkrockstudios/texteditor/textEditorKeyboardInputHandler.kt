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
import com.darkrockstudios.texteditor.state.moveCursorPageDown
import com.darkrockstudios.texteditor.state.moveCursorPageUp
import com.darkrockstudios.texteditor.state.moveCursorRight
import com.darkrockstudios.texteditor.state.moveCursorToLineEnd
import com.darkrockstudios.texteditor.state.moveCursorToLineStart
import com.darkrockstudios.texteditor.state.moveCursorUp
import com.darkrockstudios.texteditor.state.moveToNextWord
import com.darkrockstudios.texteditor.state.moveToPreviousWord

internal fun Modifier.textEditorKeyboardInputHandler(
	state: TextEditorState,
	clipboardManager: ClipboardManager
): Modifier {
	return this.onPreviewKeyEvent { keyEvent ->
		if (keyEvent.type == KeyEventType.KeyDown) {
			when {
				// Edit key combos
				keyEvent.isCtrlPressed && keyEvent.key == Key.A -> {
					state.selector.selectAll()
					true
				}

				keyEvent.isCtrlPressed && keyEvent.key == Key.C -> {
					state.selector.selection?.let {
						val selectedText = state.selector.getSelectedText()
						clipboardManager.setText(AnnotatedString(selectedText))
					}
					true
				}

				keyEvent.isCtrlPressed && keyEvent.key == Key.X -> {
					state.selector.selection?.let {
						val selectedText = state.selector.getSelectedText()
						state.selector.deleteSelection()
						clipboardManager.setText(AnnotatedString(selectedText))
					}
					true
				}

				keyEvent.isCtrlPressed && keyEvent.key == Key.V -> {
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

				keyEvent.isCtrlPressed && keyEvent.key == Key.Z -> {
					state.undo()
					true
				}

				keyEvent.isCtrlPressed && keyEvent.key == Key.Y -> {
					state.redo()
					true
				}

				keyEvent.key == Key.Delete -> {
					if (state.selector.selection != null) {
						state.selector.deleteSelection()
					} else {
						state.deleteAtCursor()
					}
					true
				}

				keyEvent.key == Key.Backspace -> {
					if (state.selector.selection != null) {
						state.selector.deleteSelection()
					} else {
						state.backspaceAtCursor()
					}
					true
				}

				keyEvent.key == Key.Enter -> {
					if (state.selector.selection != null) {
						state.selector.deleteSelection()
					}
					state.insertNewlineAtCursor()
					true
				}

				keyEvent.key == Key.DirectionLeft -> {

					if (keyEvent.isShiftPressed) {
						val initialPosition = state.cursorPosition
						if (keyEvent.isCtrlPressed)
							state.moveToPreviousWord()
						else
							state.moveCursorLeft()
						updateSelectionForCursorMovement(state, initialPosition)
					} else {
						state.selector.clearSelection()
						if (keyEvent.isCtrlPressed)
							state.moveToPreviousWord()
						else
							state.moveCursorLeft()
					}
					true
				}

				keyEvent.key == Key.DirectionRight -> {
					if (keyEvent.isShiftPressed) {
						val initialPosition = state.cursorPosition
						if (keyEvent.isCtrlPressed)
							state.moveToNextWord()
						else
							state.moveCursorRight()
						updateSelectionForCursorMovement(state, initialPosition)
					} else {
						state.selector.clearSelection()
						if (keyEvent.isCtrlPressed)
							state.moveToNextWord()
						else
							state.moveCursorRight()
					}
					true
				}

				keyEvent.key == Key.DirectionUp -> {
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

				keyEvent.key == Key.DirectionDown -> {
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

				keyEvent.key == Key.MoveHome -> {
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

				keyEvent.key == Key.MoveEnd -> {
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

				keyEvent.key == Key.PageUp -> {
					if (keyEvent.isShiftPressed) {
						val initialPosition = state.cursorPosition
						state.moveCursorPageUp()
						updateSelectionForCursorMovement(state, initialPosition)
					} else {
						state.selector.clearSelection()
						state.moveCursorPageUp()
					}
					true
				}

				keyEvent.key == Key.PageDown -> {
					if (keyEvent.isShiftPressed) {
						val initialPosition = state.cursorPosition
						state.moveCursorPageDown()
						updateSelectionForCursorMovement(state, initialPosition)
					} else {
						state.selector.clearSelection()
						state.moveCursorPageDown()
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
						state.selector.clearSelection()
						true
					} else {
						false
					}
				}
			}
		} else {
			false
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
			when (initialPosition) {
				currentSelection.start -> {
					state.selector.updateSelection(state.cursorPosition, currentSelection.end)
				}

				// If we were at the end of the selection, keep the start fixed
				currentSelection.end -> {
					state.selector.updateSelection(currentSelection.start, state.cursorPosition)
				}

				// If cursor was outside selection, create new selection
				else -> {
					state.selector.updateSelection(initialPosition, state.cursorPosition)
				}
			}
		}
	}
}