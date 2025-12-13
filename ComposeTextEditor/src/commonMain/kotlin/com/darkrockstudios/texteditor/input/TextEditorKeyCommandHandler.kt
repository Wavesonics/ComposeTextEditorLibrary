package com.darkrockstudios.texteditor.input

import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.ClipboardManager
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.state.*

/**
 * Handles keyboard commands (shortcuts and navigation) for the text editor.
 * Also handles character input for desktop platforms via KEY_TYPED events.
 */
internal class TextEditorKeyCommandHandler {

	/**
	 * Handle a key event and return true if it was consumed.
	 * This handles keyboard shortcuts and navigation on KeyDown events.
	 * @param enabled Whether the editor is enabled for editing. When false, only selection and copy operations are allowed.
	 */
	fun handleKeyEvent(
		keyEvent: KeyEvent,
		state: TextEditorState,
		clipboardManager: ClipboardManager,
		enabled: Boolean = true
	): Boolean {
		if (keyEvent.type != KeyEventType.KeyDown) return false

		return when {
			// Selection, copy and navigation operations are always allowed
			keyEvent.isCtrlPressed && keyEvent.key == Key.A -> {
				state.selector.selectAll()
				true
			}

			keyEvent.isCtrlPressed && keyEvent.key == Key.C -> {
				handleCopy(state, clipboardManager)
				true
			}

			keyEvent.key == Key.DirectionLeft -> {
				handleLeftArrow(keyEvent, state)
				true
			}

			keyEvent.key == Key.DirectionRight -> {
				handleRightArrow(keyEvent, state)
				true
			}

			keyEvent.key == Key.DirectionUp -> {
				handleUpArrow(keyEvent, state)
				true
			}

			keyEvent.key == Key.DirectionDown -> {
				handleDownArrow(keyEvent, state)
				true
			}

			keyEvent.key == Key.MoveHome -> {
				handleHome(keyEvent, state)
				true
			}

			keyEvent.key == Key.MoveEnd -> {
				handleEnd(keyEvent, state)
				true
			}

			keyEvent.key == Key.PageUp -> {
				handlePageUp(keyEvent, state)
				true
			}

			keyEvent.key == Key.PageDown -> {
				handlePageDown(keyEvent, state)
				true
			}

			// Editing operations require enabled=true
			!enabled -> false

			keyEvent.isCtrlPressed && keyEvent.key == Key.X -> {
				handleCut(state, clipboardManager)
				true
			}

			keyEvent.isCtrlPressed && keyEvent.key == Key.V -> {
				handlePaste(state, clipboardManager)
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
				handleDelete(state)
				true
			}

			keyEvent.key == Key.Backspace -> {
				handleBackspace(state)
				true
			}

			keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter -> {
				handleEnter(state)
				true
			}

			else -> false
		}
	}

	/**
	 * Handle character input events (KEY_TYPED on desktop).
	 * On desktop platforms, typed characters arrive as KeyEventType.Unknown events.
	 * Returns true if the event was consumed.
	 */
	fun handleCharacterInput(
		keyEvent: KeyEvent,
		state: TextEditorState
	): Boolean {
		// On desktop, KEY_TYPED events come through as Unknown type
		if (keyEvent.type != KeyEventType.Unknown) return false

		// Don't handle if modifier keys are pressed (except shift for uppercase)
		if (keyEvent.isCtrlPressed || keyEvent.isAltPressed || keyEvent.isMetaPressed) {
			return false
		}

		val codePoint = keyEvent.utf16CodePoint
		// Filter out control characters and invalid code points
		// Control characters are 0x00-0x1F and 0x7F-0x9F
		if (codePoint <= 0 || codePoint in 0x00..0x1F || codePoint in 0x7F..0x9F) {
			return false
		}

		// Convert code point to string (handles surrogate pairs for supplementary characters)
		val character = codePointToString(codePoint)

		// Delete selection if any, then insert character
		if (state.selector.selection != null) {
			state.selector.deleteSelection()
		}
		state.insertStringAtCursor(character)

		return true
	}

	private fun handleCopy(state: TextEditorState, clipboardManager: ClipboardManager) {
		state.selector.selection?.let {
			val selectedText = state.selector.getSelectedText()
			clipboardManager.setText(selectedText)
		}
	}

	private fun handleCut(state: TextEditorState, clipboardManager: ClipboardManager) {
		state.selector.selection?.let {
			val selectedText = state.selector.getSelectedText()
			state.selector.deleteSelection()
			clipboardManager.setText(selectedText)
		}
	}

	private fun handlePaste(state: TextEditorState, clipboardManager: ClipboardManager) {
		clipboardManager.getText()?.let { text ->
			val curSelection = state.selector.selection
			if (curSelection != null) {
				state.replace(curSelection, text)
			} else {
				state.insertStringAtCursor(text)
			}
		}
		state.selector.clearSelection()
	}

	private fun handleDelete(state: TextEditorState) {
		if (state.selector.selection != null) {
			state.selector.deleteSelection()
		} else {
			state.deleteAtCursor()
		}
	}

	private fun handleBackspace(state: TextEditorState) {
		if (state.selector.selection != null) {
			state.selector.deleteSelection()
		} else {
			state.backspaceAtCursor()
		}
	}

	private fun handleEnter(state: TextEditorState) {
		if (state.selector.selection != null) {
			state.selector.deleteSelection()
		}
		state.insertNewlineAtCursor()
	}

	private fun handleLeftArrow(keyEvent: KeyEvent, state: TextEditorState) {
		if (keyEvent.isShiftPressed) {
			val initialPosition = state.cursorPosition
			if (keyEvent.isCtrlPressed)
				state.moveToPreviousWord()
			else
				state.cursor.moveLeft()
			updateSelectionForCursorMovement(state, initialPosition)
		} else {
			state.selector.clearSelection()
			if (keyEvent.isCtrlPressed)
				state.moveToPreviousWord()
			else
				state.cursor.moveLeft()
		}
	}

	private fun handleRightArrow(keyEvent: KeyEvent, state: TextEditorState) {
		if (keyEvent.isShiftPressed) {
			val initialPosition = state.cursorPosition
			if (keyEvent.isCtrlPressed)
				state.moveToNextWord()
			else
				state.cursor.moveRight()
			updateSelectionForCursorMovement(state, initialPosition)
		} else {
			state.selector.clearSelection()
			if (keyEvent.isCtrlPressed)
				state.moveToNextWord()
			else
				state.cursor.moveRight()
		}
	}

	private fun handleUpArrow(keyEvent: KeyEvent, state: TextEditorState) {
		if (keyEvent.isShiftPressed) {
			val initialPosition = state.cursorPosition
			state.moveCursorUp()
			updateSelectionForCursorMovement(state, initialPosition)
		} else {
			state.selector.clearSelection()
			state.moveCursorUp()
		}
	}

	private fun handleDownArrow(keyEvent: KeyEvent, state: TextEditorState) {
		if (keyEvent.isShiftPressed) {
			val initialPosition = state.cursorPosition
			state.moveCursorDown()
			updateSelectionForCursorMovement(state, initialPosition)
		} else {
			state.selector.clearSelection()
			state.moveCursorDown()
		}
	}

	private fun handleHome(keyEvent: KeyEvent, state: TextEditorState) {
		if (keyEvent.isShiftPressed) {
			val initialPosition = state.cursorPosition
			state.cursor.moveToLineStart()
			updateSelectionForCursorMovement(state, initialPosition)
		} else {
			state.selector.clearSelection()
			state.cursor.moveToLineStart()
		}
	}

	private fun handleEnd(keyEvent: KeyEvent, state: TextEditorState) {
		if (keyEvent.isShiftPressed) {
			val initialPosition = state.cursorPosition
			state.moveCursorToLineEnd()
			updateSelectionForCursorMovement(state, initialPosition)
		} else {
			state.selector.clearSelection()
			state.moveCursorToLineEnd()
		}
	}

	private fun handlePageUp(keyEvent: KeyEvent, state: TextEditorState) {
		if (keyEvent.isShiftPressed) {
			val initialPosition = state.cursorPosition
			state.moveCursorPageUp()
			updateSelectionForCursorMovement(state, initialPosition)
		} else {
			state.selector.clearSelection()
			state.moveCursorPageUp()
		}
	}

	private fun handlePageDown(keyEvent: KeyEvent, state: TextEditorState) {
		if (keyEvent.isShiftPressed) {
			val initialPosition = state.cursorPosition
			state.moveCursorPageDown()
			updateSelectionForCursorMovement(state, initialPosition)
		} else {
			state.selector.clearSelection()
			state.moveCursorPageDown()
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

	/**
	 * Converts a Unicode code point to a String.
	 * Handles supplementary characters (code points > 0xFFFF) by creating surrogate pairs.
	 */
	private fun codePointToString(codePoint: Int): String {
		return if (codePoint <= 0xFFFF) {
			// Basic Multilingual Plane - single char
			codePoint.toChar().toString()
		} else {
			// Supplementary character - needs surrogate pair
			val adjusted = codePoint - 0x10000
			val highSurrogate = ((adjusted shr 10) + 0xD800).toChar()
			val lowSurrogate = ((adjusted and 0x3FF) + 0xDC00).toChar()
			"$highSurrogate$lowSurrogate"
		}
	}
}
