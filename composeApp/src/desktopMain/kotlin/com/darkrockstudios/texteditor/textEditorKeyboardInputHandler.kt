package com.darkrockstudios.texteditor

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.moveCursorDown
import com.darkrockstudios.texteditor.state.moveCursorLeft
import com.darkrockstudios.texteditor.state.moveCursorRight
import com.darkrockstudios.texteditor.state.moveCursorToLineEnd
import com.darkrockstudios.texteditor.state.moveCursorToLineStart
import com.darkrockstudios.texteditor.state.moveCursorUp

fun Modifier.textEditorKeyboardInputHandler(state: TextEditorState): Modifier {
	return this.onPreviewKeyEvent {
		if (it.type == KeyEventType.KeyDown) {
			val initialPosition = state.cursorPosition

			return@onPreviewKeyEvent when {
				// Selection handling with shift held
				it.isShiftPressed && it.key == Key.DirectionLeft -> {
					state.moveCursorLeft()
					updateSelectionForCursorMovement(state, initialPosition)
					true
				}

				it.isShiftPressed && it.key == Key.DirectionRight -> {
					state.moveCursorRight()
					updateSelectionForCursorMovement(state, initialPosition)
					true
				}

				it.isShiftPressed && it.key == Key.DirectionUp -> {
					state.moveCursorUp()
					updateSelectionForCursorMovement(state, initialPosition)
					true
				}

				it.isShiftPressed && it.key == Key.DirectionDown -> {
					state.moveCursorDown()
					updateSelectionForCursorMovement(state, initialPosition)
					true
				}

				it.isShiftPressed && it.key == Key.MoveHome -> {
					state.moveCursorToLineStart()
					updateSelectionForCursorMovement(state, initialPosition)
					true
				}

				it.isShiftPressed && (it.key == Key.MoveEnd) -> {
					state.moveCursorToLineEnd()
					updateSelectionForCursorMovement(state, initialPosition)
					true
				}

				// Regular cursor movement (clear selection)
				it.key == Key.DirectionLeft -> {
					state.selector.clearSelection()
					state.moveCursorLeft()
					true
				}

				it.key == Key.DirectionRight -> {
					state.selector.clearSelection()
					state.moveCursorRight()
					true
				}

				it.key == Key.DirectionUp -> {
					state.selector.clearSelection()
					state.moveCursorUp()
					true
				}

				it.key == Key.DirectionDown -> {
					state.selector.clearSelection()
					state.moveCursorDown()
					true
				}

				it.key == Key.MoveHome -> {
					state.selector.clearSelection()
					state.moveCursorToLineStart()
					true
				}

				it.key == Key.MoveEnd -> {
					state.selector.clearSelection()
					state.moveCursorToLineEnd()
					true
				}

				// Character deletion (handle selection)
				it.key == Key.Delete -> {
					if (state.selector.selection != null) {
						state.selector.deleteSelection()
					} else {
						state.deleteAtCursor()
					}
					true
				}

				it.key == Key.Backspace -> {
					if (state.selector.selection != null) {
						state.selector.deleteSelection()
					} else {
						state.backspaceAtCursor()
					}
					true
				}

				// Other cases remain the same
				it.key == Key.Enter -> {
					if (state.selector.selection != null) {
						state.selector.deleteSelection()
					}
					state.insertNewlineAtCursor()
					true
				}

				else -> {
					val char = keyEventToUTF8Character(it)
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

private fun updateSelectionForCursorMovement(state: TextEditorState, initialPosition: TextOffset) {
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