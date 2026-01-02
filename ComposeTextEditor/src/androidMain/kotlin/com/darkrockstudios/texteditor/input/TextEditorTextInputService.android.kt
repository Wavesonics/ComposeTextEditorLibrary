package com.darkrockstudios.texteditor.input

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.*
import androidx.annotation.RequiresApi
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
		// Using TYPE_TEXT_VARIATION_NORMAL explicitly for standard text behavior
		outAttributes.inputType = InputType.TYPE_CLASS_TEXT or
				InputType.TYPE_TEXT_VARIATION_NORMAL or
				InputType.TYPE_TEXT_FLAG_MULTI_LINE

		// Enable autocomplete and suggestions, prevent fullscreen mode
		outAttributes.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or
				EditorInfo.IME_FLAG_NO_EXTRACT_UI or
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
	// @Volatile ensures visibility across threads since IME callbacks may come from different threads
	@Volatile
	private var composingStart: Int = -1
	@Volatile
	private var composingEnd: Int = -1

	// Track what position the IME thinks the cursor is at
	// This helps us handle relative cursor movements correctly when IME has stale data
	@Volatile
	private var imeExpectedCursorPos: Int = state.getCharacterIndex(state.cursorPosition)

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

		// Update the expected cursor position for IME sync
		imeExpectedCursorPos = state.getCharacterIndex(state.cursorPosition)

		return true
	}

	override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
		if (text == null) return false

		when {
			// If we have existing non-empty composing text, replace it
			composingStart >= 0 && composingEnd > composingStart -> {
				val startOffset = state.getOffsetAtCharacter(composingStart)
				val endOffset = state.getOffsetAtCharacter(composingEnd)
				val range = TextEditorRange(startOffset, endOffset)
				state.replace(range, text.toString())

				// Update composing range end
				composingEnd = composingStart + text.length
			}

			// Empty composing region (composingStart == composingEnd) - insert at that position
			// Some IMEs set empty composing regions before inserting text
			composingStart >= 0 && composingEnd == composingStart -> {
				// Delete selection if present
				if (state.selector.hasSelection()) {
					state.selector.deleteSelection()
				}

				// Move cursor to composing position and insert
				val insertOffset = state.getOffsetAtCharacter(composingStart)
				state.cursor.updatePosition(insertOffset)
				state.insertStringAtCursor(text.toString())
				composingEnd = composingStart + text.length
			}

			// No composing region - insert at current cursor position
			else -> {
				// Delete selection if present
				if (state.selector.hasSelection()) {
					state.selector.deleteSelection()
				}

				// Insert composing text at cursor
				composingStart = state.getCharacterIndex(state.cursorPosition)
				state.insertStringAtCursor(text.toString())
				composingEnd = composingStart + text.length
			}
		}

		// Update the expected cursor position for IME sync
		imeExpectedCursorPos = state.getCharacterIndex(state.cursorPosition)

		// Notify state about composing region for visual feedback
		state.updateComposingRange(composingStart, composingEnd)

		return true
	}

	override fun finishComposingText(): Boolean {
		// Clear composing state
		composingStart = -1
		composingEnd = -1
		// Clear visual feedback
		state.clearComposingRange()
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

		// Update the expected cursor position for IME sync
		imeExpectedCursorPos = state.getCharacterIndex(state.cursorPosition)

		return true
	}

	override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
		val cursorIndex = state.getCharacterIndex(state.cursorPosition)
		val fullText = state.getAllText()
		val textLength = fullText.length

		// Convert code points to character counts, handling surrogate pairs
		val charsBefore = codePointsToChars(fullText, cursorIndex, beforeLength, backwards = true)
		val charsAfter = codePointsToChars(fullText, cursorIndex, afterLength, backwards = false)

		// Calculate delete range in characters
		val deleteStart = maxOf(0, cursorIndex - charsBefore)
		val deleteEnd = minOf(textLength, cursorIndex + charsAfter)

		if (deleteStart < deleteEnd) {
			val startOffset = state.getOffsetAtCharacter(deleteStart)
			val endOffset = state.getOffsetAtCharacter(deleteEnd)
			val range = TextEditorRange(startOffset, endOffset)
			state.delete(range)
		}

		// Update the expected cursor position for IME sync
		imeExpectedCursorPos = state.getCharacterIndex(state.cursorPosition)

		return true
	}

	/**
	 * Converts a count of code points to a count of UTF-16 characters (code units).
	 * Handles surrogate pairs correctly for emoji and characters outside the BMP.
	 *
	 * @param text The full text
	 * @param fromIndex Starting character index
	 * @param codePointCount Number of code points to count
	 * @param backwards If true, count backwards from fromIndex; if false, count forwards
	 * @return Number of UTF-16 characters that represent the given code points
	 */
	private fun codePointsToChars(
		text: CharSequence,
		fromIndex: Int,
		codePointCount: Int,
		backwards: Boolean
	): Int {
		if (codePointCount <= 0) return 0

		var charCount = 0
		var codePointsRemaining = codePointCount

		if (backwards) {
			var index = fromIndex
			while (codePointsRemaining > 0 && index > 0) {
				index--
				charCount++
				// Check if this is the low surrogate of a surrogate pair
				if (Character.isLowSurrogate(text[index]) && index > 0 &&
					Character.isHighSurrogate(text[index - 1])
				) {
					// This is part of a surrogate pair, include the high surrogate too
					index--
					charCount++
				}
				codePointsRemaining--
			}
		} else {
			var index = fromIndex
			while (codePointsRemaining > 0 && index < text.length) {
				charCount++
				// Check if this is the high surrogate of a surrogate pair
				if (Character.isHighSurrogate(text[index]) && index + 1 < text.length &&
					Character.isLowSurrogate(text[index + 1])
				) {
					// This is a surrogate pair, include both characters
					charCount++
					index++
				}
				index++
				codePointsRemaining--
			}
		}

		return charCount
	}

	// ============ SELECTION METHODS ============

	override fun setSelection(start: Int, end: Int): Boolean {
		// For cursor movement (not selection), detect if IME is trying to move
		// relative to a stale position and apply the delta to our actual position
		if (start == end) {
			val actualCursorPos = state.getCharacterIndex(state.cursorPosition)
			val delta = start - imeExpectedCursorPos

			// Determine if this looks like a relative move or absolute positioning
			// Relative moves: IME requests position relative to what it thinks cursor is at
			// Absolute positioning: IME requests specific position (e.g., user tap, select word)
			val newPos = when {
				// Zero delta means IME thinks cursor is already there - no move needed
				delta == 0 -> start.coerceIn(0, state.getTextLength())

				// Common relative move deltas from arrow keys and word movement:
				// -1, +1: single character left/right
				// -2 to -20, +2 to +20: word movement (varies by word length)
				// Larger values are more likely to be absolute positioning
				isLikelyRelativeMove(delta) -> {
					// Apply delta to actual position to handle stale IME cursor data
					(actualCursorPos + delta).coerceIn(0, state.getTextLength())
				}

				// Large delta or IME expected position matches actual - use absolute
				else -> start.coerceIn(0, state.getTextLength())
			}

			val offset = state.getOffsetAtCharacter(newPos)
			state.cursor.updatePosition(offset)
			state.selector.clearSelection()

			// Update expected position to where IME thinks cursor is now
			imeExpectedCursorPos = start
		} else {
			// Selection (start != end) - use absolute positions
			val startOffset = state.getOffsetAtCharacter(start.coerceIn(0, state.getTextLength()))
			val endOffset = state.getOffsetAtCharacter(end.coerceIn(0, state.getTextLength()))
			state.selector.updateSelection(startOffset, endOffset)
			state.cursor.updatePosition(endOffset)
			imeExpectedCursorPos = end
		}

		return true
	}

	/**
	 * Determines if a cursor movement delta looks like a relative move vs absolute positioning.
	 *
	 * Relative moves come from keyboard navigation:
	 * - Arrow keys: ±1 character
	 * - Ctrl+Arrow (word movement): typically ±2 to ±20 depending on word length
	 * - Home/End within a line: could be larger but usually < 100
	 *
	 * Absolute positioning comes from:
	 * - User tapping to a specific location
	 * - IME selecting a suggestion
	 * - Document navigation (Ctrl+Home/End)
	 *
	 * We use a threshold that covers most navigation while excluding likely absolute positions.
	 * A threshold of 50 covers typical line lengths for word navigation.
	 */
	private fun isLikelyRelativeMove(delta: Int): Boolean {
		val absDelta = kotlin.math.abs(delta)
		// Single char moves are definitely relative
		if (absDelta == 1) return true
		// Word/line navigation is typically under 50 chars
		// Larger moves are more likely to be absolute positioning
		return absDelta in 2..50
	}

	// ============ CURSOR/COMPOSING EXTRACTION ============

	override fun getCursorCapsMode(reqModes: Int): Int {
		if (reqModes == 0) return 0

		val cursorIndex = state.getCharacterIndex(state.cursorPosition)

		// At the very start of the document, suggest sentence caps
		if (cursorIndex == 0) {
			return reqModes and (android.text.TextUtils.CAP_MODE_CHARACTERS or
					android.text.TextUtils.CAP_MODE_WORDS or
					android.text.TextUtils.CAP_MODE_SENTENCES)
		}

		// Get text before cursor for analysis
		val textBefore = getTextBeforeCursor(cursorIndex, 0) ?: return 0

		// Use Android's TextUtils to determine caps mode
		return android.text.TextUtils.getCapsMode(textBefore, textBefore.length, reqModes)
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

	@RequiresApi(Build.VERSION_CODES.S)
	override fun getSurroundingText(
		beforeLength: Int,
		afterLength: Int,
		flags: Int
	): SurroundingText {
		val cursorIndex = state.getCharacterIndex(state.cursorPosition)
		val textLength = state.getTextLength()

		// Calculate the range of text to return
		val start = maxOf(0, cursorIndex - beforeLength)
		val end = minOf(textLength, cursorIndex + afterLength)

		// Get the text in the range
		val text = if (start < end) {
			state.getAllText().subSequence(start, end).toString()
		} else {
			""
		}

		// Calculate selection within the returned text
		val selection = state.selector.selection
		val selectionStart: Int
		val selectionEnd: Int

		if (selection != null) {
			val selStart = state.getCharacterIndex(selection.start)
			val selEnd = state.getCharacterIndex(selection.end)
			// Adjust selection positions relative to the returned text's start
			selectionStart = (selStart - start).coerceIn(0, text.length)
			selectionEnd = (selEnd - start).coerceIn(0, text.length)
		} else {
			// No selection, cursor position relative to returned text
			selectionStart = (cursorIndex - start).coerceIn(0, text.length)
			selectionEnd = selectionStart
		}

		return SurroundingText(text, selectionStart, selectionEnd, start)
	}

	// ============ COMPOSING REGION METHODS ============

	override fun setComposingRegion(start: Int, end: Int): Boolean {
		composingStart = start
		composingEnd = end
		// Notify state about composing region for visual feedback
		state.updateComposingRange(composingStart, composingEnd)
		return true
	}

	// ============ OTHER REQUIRED METHODS ============

	override fun beginBatchEdit(): Boolean {
		state.platformExtensions.beginBatchEdit()
		return true
	}

	override fun endBatchEdit(): Boolean {
		val batchEnded = state.platformExtensions.endBatchEdit()
		if (batchEnded) {
			// Force an IME selection update now that the batch has ended
			// The IME cursor sync may have skipped updates during the batch
			notifyImeSelectionChanged()
		}
		return true
	}

	/**
	 * Notifies the IME of the current selection/cursor position.
	 * Called when a batch edit ends to ensure the IME has the final state.
	 */
	private fun notifyImeSelectionChanged() {
		val view = state.platformExtensions.view ?: return
		val imm = view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
			as? android.view.inputmethod.InputMethodManager ?: return

		val cursorIndex = state.getCharacterIndex(state.cursorPosition)
		val selection = state.selector.selection
		val selStart: Int
		val selEnd: Int

		if (selection != null) {
			selStart = state.getCharacterIndex(selection.start)
			selEnd = state.getCharacterIndex(selection.end)
		} else {
			selStart = cursorIndex
			selEnd = cursorIndex
		}

		// Get composing region if active
		val composingRange = state.composingRange
		val candidatesStart = composingRange?.let { state.getCharacterIndex(it.start) } ?: -1
		val candidatesEnd = composingRange?.let { state.getCharacterIndex(it.end) } ?: -1

		imm.updateSelection(view, selStart, selEnd, candidatesStart, candidatesEnd)
	}

	override fun clearMetaKeyStates(states: Int): Boolean = true

	override fun sendKeyEvent(event: KeyEvent?): Boolean {
		if (event == null) return false

		// Only handle key down events
		if (event.action != KeyEvent.ACTION_DOWN) return false

		val handled = when (event.keyCode) {
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

		// Update the expected cursor position for IME sync after any handled key event
		if (handled) {
			imeExpectedCursorPos = state.getCharacterIndex(state.cursorPosition)
		}

		return handled
	}

	override fun performEditorAction(editorAction: Int): Boolean {
		// Handle IME action button (e.g., "Done", "Go", "Search")
		// For now, just return false
		return false
	}

	override fun performContextMenuAction(id: Int): Boolean = false

	override fun performPrivateCommand(action: String?, data: Bundle?): Boolean = false

	override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
		// Set monitoring flag for continuous updates
		state.platformExtensions.cursorAnchorMonitoringEnabled =
			(cursorUpdateMode and InputConnection.CURSOR_UPDATE_MONITOR) != 0

		// If immediate update requested, send cursor info now
		if (cursorUpdateMode and InputConnection.CURSOR_UPDATE_IMMEDIATE != 0) {
			state.platformExtensions.sendCursorAnchorInfo()
		}

		// Return true to indicate we support cursor updates
		return true
	}

	override fun getHandler(): Handler? = null

	override fun closeConnection() {
		finishComposingText()
		// Stop monitoring cursor updates when connection closes
		state.platformExtensions.cursorAnchorMonitoringEnabled = false
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
