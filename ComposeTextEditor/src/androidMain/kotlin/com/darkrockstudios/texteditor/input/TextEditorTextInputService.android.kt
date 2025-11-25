package com.darkrockstudios.texteditor.input

import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.moveCursorDown
import com.darkrockstudios.texteditor.state.moveCursorToLineEnd
import com.darkrockstudios.texteditor.state.moveCursorUp

/**
 * Android implementation of TextEditorTextInputService.
 * This creates a PlatformTextInputMethodRequest that provides an InputConnection
 * to bridge the Android IME (soft keyboard) to the TextEditorState.
 */
actual class TextEditorTextInputService actual constructor(
	private val state: TextEditorState
) {
	actual suspend fun startInput(session: PlatformTextInputSession): Nothing {
		val request = TextEditorInputMethodRequest(state)
		session.startInputMethod(request)
	}
}

/**
 * Android-specific implementation of PlatformTextInputMethodRequest.
 * Creates an InputConnection that bridges the IME to TextEditorState.
 */
private class TextEditorInputMethodRequest(
	private val state: TextEditorState
) : PlatformTextInputMethodRequest {

	override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
		configureEditorInfo(outAttributes)
		return TextEditorInputConnection(state)
	}

	private fun configureEditorInfo(outAttributes: EditorInfo) {
		// Basic text input with multi-line support
		outAttributes.inputType = InputType.TYPE_CLASS_TEXT or
				InputType.TYPE_TEXT_FLAG_MULTI_LINE or
				InputType.TYPE_TEXT_FLAG_AUTO_CORRECT

		// Enable autocomplete and suggestions, prevent fullscreen mode
		outAttributes.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or
				EditorInfo.IME_ACTION_NONE

		// Set initial selection/cursor position
		val cursorPos = state.getCharacterIndex(state.cursorPosition)
		val selection = state.selector.selection

		if (selection != null) {
			outAttributes.initialSelStart = state.getCharacterIndex(selection.start)
			outAttributes.initialSelEnd = state.getCharacterIndex(selection.end)
		} else {
			outAttributes.initialSelStart = cursorPos
			outAttributes.initialSelEnd = cursorPos
		}
	}
}

/**
 * Custom InputConnection that bridges Android IME events to TextEditorState.
 */
private class TextEditorInputConnection(
	private val state: TextEditorState
) : InputConnection {

	// Composing text state (for autocomplete/suggestions preview)
	private var composingStart: Int = -1
	private var composingEnd: Int = -1

	// ============ TEXT RETRIEVAL METHODS ============

	override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
		val cursorIndex = state.getCharacterIndex(state.cursorPosition)
		val start = maxOf(0, cursorIndex - n)

		return if (start < cursorIndex) {
			state.getAllText().subSequence(start, cursorIndex)
		} else {
			""
		}
	}

	override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
		val cursorIndex = state.getCharacterIndex(state.cursorPosition)
		val textLength = state.getTextLength()
		val end = minOf(textLength, cursorIndex + n)

		return if (cursorIndex < end) {
			state.getAllText().subSequence(cursorIndex, end)
		} else {
			""
		}
	}

	override fun getSelectedText(flags: Int): CharSequence? {
		return state.selector.selection?.let { selection ->
			state.getStringInRange(selection)
		}
	}

	// ============ TEXT MODIFICATION METHODS ============

	override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
		if (text == null) return false

		// Finish any composing text first
		finishComposingText()

		// Delete selection if present
		if (state.selector.hasSelection()) {
			state.selector.deleteSelection()
		}

		// Insert the committed text
		state.insertStringAtCursor(text.toString())

		return true
	}

	override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
		if (text == null) return false

		// If we have existing composing text, replace it
		if (composingStart >= 0 && composingEnd >= 0 && composingStart < composingEnd) {
			val startOffset = state.getOffsetAtCharacter(composingStart)
			val endOffset = state.getOffsetAtCharacter(composingEnd)
			val range = TextEditorRange(startOffset, endOffset)
			state.replace(range, text.toString())

			// Update composing range
			composingEnd = composingStart + text.length
		} else {
			// Delete selection if present
			if (state.selector.hasSelection()) {
				state.selector.deleteSelection()
			}

			// Insert composing text
			composingStart = state.getCharacterIndex(state.cursorPosition)
			state.insertStringAtCursor(text.toString())
			composingEnd = composingStart + text.length
		}

		return true
	}

	override fun finishComposingText(): Boolean {
		// Clear composing state
		composingStart = -1
		composingEnd = -1
		return true
	}

	override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
		val cursorIndex = state.getCharacterIndex(state.cursorPosition)

		// Calculate delete range
		val deleteStart = maxOf(0, cursorIndex - beforeLength)
		val deleteEnd = minOf(state.getTextLength(), cursorIndex + afterLength)

		if (deleteStart < deleteEnd) {
			val startOffset = state.getOffsetAtCharacter(deleteStart)
			val endOffset = state.getOffsetAtCharacter(deleteEnd)
			val range = TextEditorRange(startOffset, endOffset)
			state.delete(range)
		}

		return true
	}

	override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
		// For simplicity, treat code points as characters
		// A more complete implementation would handle surrogate pairs
		return deleteSurroundingText(beforeLength, afterLength)
	}

	// ============ SELECTION METHODS ============

	override fun setSelection(start: Int, end: Int): Boolean {
		val startOffset = state.getOffsetAtCharacter(start)
		val endOffset = state.getOffsetAtCharacter(end)

		if (start == end) {
			state.cursor.updatePosition(startOffset)
			state.selector.clearSelection()
		} else {
			state.selector.updateSelection(startOffset, endOffset)
			state.cursor.updatePosition(endOffset)
		}

		return true
	}

	// ============ CURSOR/COMPOSING EXTRACTION ============

	override fun getCursorCapsMode(reqModes: Int): Int {
		// Return capitalization mode based on cursor position
		// For simplicity, return 0 (no special caps mode)
		return 0
	}

	override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
		if (request == null) return null

		return ExtractedText().apply {
			text = state.getAllText()
			startOffset = 0

			val cursorIndex = state.getCharacterIndex(state.cursorPosition)
			val selection = state.selector.selection

			if (selection != null) {
				selectionStart = state.getCharacterIndex(selection.start)
				selectionEnd = state.getCharacterIndex(selection.end)
			} else {
				selectionStart = cursorIndex
				selectionEnd = cursorIndex
			}
		}
	}

	// ============ COMPOSING REGION METHODS ============

	override fun setComposingRegion(start: Int, end: Int): Boolean {
		composingStart = start
		composingEnd = end
		return true
	}

	// ============ OTHER REQUIRED METHODS ============

	override fun beginBatchEdit(): Boolean = true

	override fun endBatchEdit(): Boolean = true

	override fun clearMetaKeyStates(states: Int): Boolean = true

	override fun sendKeyEvent(event: KeyEvent?): Boolean {
		if (event == null) return false

		// Only handle key down events
		if (event.action != KeyEvent.ACTION_DOWN) return false

		return when (event.keyCode) {
			KeyEvent.KEYCODE_DPAD_LEFT -> {
				state.cursor.moveLeft()
				true
			}

			KeyEvent.KEYCODE_DPAD_RIGHT -> {
				state.cursor.moveRight()
				true
			}

			KeyEvent.KEYCODE_DPAD_UP -> {
				state.moveCursorUp()
				true
			}

			KeyEvent.KEYCODE_DPAD_DOWN -> {
				state.moveCursorDown()
				true
			}

			KeyEvent.KEYCODE_MOVE_HOME -> {
				state.cursor.moveToLineStart()
				true
			}

			KeyEvent.KEYCODE_MOVE_END -> {
				state.moveCursorToLineEnd()
				true
			}

			KeyEvent.KEYCODE_DEL -> {
				// Backspace
				if (state.selector.hasSelection()) {
					state.selector.deleteSelection()
				} else {
					state.backspaceAtCursor()
				}
				true
			}

			KeyEvent.KEYCODE_FORWARD_DEL -> {
				// Delete
				if (state.selector.hasSelection()) {
					state.selector.deleteSelection()
				} else {
					state.deleteAtCursor()
				}
				true
			}

			KeyEvent.KEYCODE_ENTER -> {
				if (state.selector.hasSelection()) {
					state.selector.deleteSelection()
				}
				state.insertNewlineAtCursor()
				true
			}

			else -> false
		}
	}

	override fun performEditorAction(editorAction: Int): Boolean {
		// Handle IME action button (e.g., "Done", "Go", "Search")
		// For now, just return false
		return false
	}

	override fun performContextMenuAction(id: Int): Boolean = false

	override fun performPrivateCommand(action: String?, data: Bundle?): Boolean = false

	override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean = true

	override fun getHandler(): Handler? = null

	override fun closeConnection() {
		finishComposingText()
	}

	override fun commitCompletion(text: android.view.inputmethod.CompletionInfo?): Boolean = false

	override fun commitCorrection(correctionInfo: android.view.inputmethod.CorrectionInfo?): Boolean = false

	override fun reportFullscreenMode(enabled: Boolean): Boolean = false

	override fun commitContent(
		inputContentInfo: android.view.inputmethod.InputContentInfo,
		flags: Int,
		opts: Bundle?
	): Boolean {
		// Handle rich content input (images, GIFs, etc.)
		// For now, we don't support rich content insertion
		return false
	}
}
