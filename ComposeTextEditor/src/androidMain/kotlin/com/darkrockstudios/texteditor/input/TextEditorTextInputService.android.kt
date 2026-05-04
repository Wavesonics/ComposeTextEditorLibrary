package com.darkrockstudios.texteditor.input

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.text.TextUtils
import android.view.KeyEvent
import android.view.inputmethod.*
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.moveCursorDown
import com.darkrockstudios.texteditor.state.moveCursorToLineEnd
import com.darkrockstudios.texteditor.state.moveCursorUp

/**
 * Android implementation of TextEditorTextInputService.
 * Creates a PlatformTextInputMethodRequest that provides an InputConnection
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

private class TextEditorInputMethodRequest(
	private val state: TextEditorState
) : PlatformTextInputMethodRequest {

	override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
		configureEditorInfo(outAttributes)
		return TextEditorInputConnection(state)
	}

	private fun configureEditorInfo(outAttributes: EditorInfo) {
		outAttributes.inputType = InputType.TYPE_CLASS_TEXT or
				InputType.TYPE_TEXT_VARIATION_NORMAL or
				InputType.TYPE_TEXT_FLAG_MULTI_LINE or
				InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or
				InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

		outAttributes.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or
				EditorInfo.IME_FLAG_NO_EXTRACT_UI

		outAttributes.imeOptions = outAttributes.imeOptions or EditorInfo.IME_ACTION_UNSPECIFIED

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
 * InputConnection that bridges Android IME events to TextEditorState.
 *
 * Mirrors the structure of androidx.compose.foundation.text.input.internal.RecordingInputConnection
 * and StatelessInputConnection — in particular: edit operations are queued during a batch edit
 * and applied atomically when the batch completes; setSelection is treated as absolute
 * (the IME's view is kept in sync via ImeCursorSync); state.composingRange is the single
 * source of truth for composition.
 */
private class TextEditorInputConnection(
	private val state: TextEditorState
) : InputConnection {

	@Volatile
	private var isActive: Boolean = true

	/**
	 * Queue of edit operations recorded during a batch edit. When the outermost batch
	 * ends the operations are drained and applied in order, then a single
	 * `updateSelection` (and, if the IME requested it, `updateExtractedText`) is sent.
	 */
	private val pendingOps = mutableListOf<() -> Unit>()

	private inline fun ensureActive(block: () -> Unit): Boolean {
		if (!isActive) return false
		block()
		return true
	}

	private inline fun runOrQueue(crossinline op: () -> Unit) {
		if (state.platformExtensions.isInBatchEdit) {
			pendingOps += { op() }
		} else {
			op()
		}
	}

	// ============ TEXT RETRIEVAL ============

	override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence {
		val cursorIndex = state.getCharacterIndex(state.cursorPosition)
		val start = maxOf(0, cursorIndex - n)
		return if (start < cursorIndex) {
			state.getAllText().subSequence(start, cursorIndex)
		} else {
			""
		}
	}

	override fun getTextAfterCursor(n: Int, flags: Int): CharSequence {
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
		// Per Chromium / AndroidX convention: return null (not empty) when collapsed.
		return state.selector.selection?.let { state.getStringInRange(it) }
	}

	// ============ TEXT MUTATION ============

	override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean = ensureActive {
		// Per Android contract: returning false signals a dead connection. Nullable text
		// is a no-op but the connection is still valid.
		if (text == null) return@ensureActive
		runOrQueue {
			val insertStart = replaceComposingOrInsert(text.toString())
			val insertEnd = insertStart + text.length
			state.clearComposingRange()
			applyNewCursorPosition(insertStart, insertEnd, newCursorPosition)
		}
	}

	override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean = ensureActive {
		if (text == null) return@ensureActive
		runOrQueue {
			val insertStart = replaceComposingOrInsert(text.toString())
			val insertEnd = insertStart + text.length
			if (text.isNotEmpty()) {
				state.updateComposingRange(insertStart, insertEnd)
			} else {
				state.clearComposingRange()
			}
			applyNewCursorPosition(insertStart, insertEnd, newCursorPosition)
		}
	}

	/**
	 * Applies the inserted text either by replacing the current composing region or
	 * (after deleting any selection) inserting at the cursor.
	 *
	 * @return the character index at which the new text starts.
	 */
	private fun replaceComposingOrInsert(text: String): Int {
		val composing = state.composingRange
		return if (composing != null) {
			val start = state.getCharacterIndex(composing.start)
			val range = TextEditorRange(composing.start, composing.end)
			// inheritStyle keeps autocorrect from stripping bold/italic etc.
			state.replace(range, text, inheritStyle = true)
			start
		} else {
			if (state.selector.hasSelection()) {
				state.selector.deleteSelection()
			}
			val start = state.getCharacterIndex(state.cursorPosition)
			state.insertStringAtCursor(text)
			start
		}
	}

	/**
	 * Implements the Android `newCursorPosition` contract:
	 * - `> 0`: position is relative to the end of the inserted text (1 = right after).
	 * - `<= 0`: position is relative to the start (0 = at start, -1 = one before).
	 */
	private fun applyNewCursorPosition(insertStart: Int, insertEnd: Int, newCursorPosition: Int) {
		val len = state.getTextLength()
		val target = if (newCursorPosition > 0) {
			(insertEnd + (newCursorPosition - 1)).coerceIn(0, len)
		} else {
			(insertStart + newCursorPosition).coerceIn(0, len)
		}
		state.cursor.updatePosition(state.getOffsetAtCharacter(target))
		state.selector.clearSelection()
	}

	override fun finishComposingText(): Boolean = ensureActive {
		runOrQueue {
			state.clearComposingRange()
		}
	}

	override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean = ensureActive {
		runOrQueue {
			val cursorIndex = state.getCharacterIndex(state.cursorPosition)
			val deleteStart = maxOf(0, cursorIndex - beforeLength)
			val deleteEnd = minOf(state.getTextLength(), cursorIndex + afterLength)
			if (deleteStart < deleteEnd) {
				val range = TextEditorRange(
					state.getOffsetAtCharacter(deleteStart),
					state.getOffsetAtCharacter(deleteEnd)
				)
				state.delete(range)
			}
		}
	}

	override fun deleteSurroundingTextInCodePoints(
		beforeLength: Int,
		afterLength: Int
	): Boolean = ensureActive {
		runOrQueue {
			val cursorIndex = state.getCharacterIndex(state.cursorPosition)
			val fullText = state.getAllText()
			val charsBefore = codePointsToChars(fullText, cursorIndex, beforeLength, backwards = true)
			val charsAfter = codePointsToChars(fullText, cursorIndex, afterLength, backwards = false)
			val deleteStart = maxOf(0, cursorIndex - charsBefore)
			val deleteEnd = minOf(state.getTextLength(), cursorIndex + charsAfter)
			if (deleteStart < deleteEnd) {
				val range = TextEditorRange(
					state.getOffsetAtCharacter(deleteStart),
					state.getOffsetAtCharacter(deleteEnd)
				)
				state.delete(range)
			}
		}
	}

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
				if (Character.isLowSurrogate(text[index]) && index > 0 &&
					Character.isHighSurrogate(text[index - 1])
				) {
					index--
					charCount++
				}
				codePointsRemaining--
			}
		} else {
			var index = fromIndex
			while (codePointsRemaining > 0 && index < text.length) {
				charCount++
				if (Character.isHighSurrogate(text[index]) && index + 1 < text.length &&
					Character.isLowSurrogate(text[index + 1])
				) {
					charCount++
					index++
				}
				index++
				codePointsRemaining--
			}
		}
		return charCount
	}

	// ============ SELECTION & COMPOSING ============

	override fun setSelection(start: Int, end: Int): Boolean = ensureActive {
		// AndroidX contract: setSelection is ALWAYS absolute. The previous heuristic that
		// applied `start - imeExpectedCursorPos` as a delta to the actual cursor caused
		// drift whenever the IME's tracker disagreed with us. We now rely on
		// ImeCursorSync to keep the IME in sync after any of our own edits.
		runOrQueue {
			val len = state.getTextLength()
			val s = start.coerceIn(0, len)
			val e = end.coerceIn(0, len)
			if (s == e) {
				state.selector.clearSelection()
				state.cursor.updatePosition(state.getOffsetAtCharacter(s))
			} else {
				val lo = minOf(s, e)
				val hi = maxOf(s, e)
				state.selector.updateSelection(
					state.getOffsetAtCharacter(lo),
					state.getOffsetAtCharacter(hi)
				)
				// Cursor goes to the `end` arg per platform convention.
				state.cursor.updatePosition(state.getOffsetAtCharacter(e))
			}
		}
	}

	override fun setComposingRegion(start: Int, end: Int): Boolean = ensureActive {
		runOrQueue {
			val len = state.getTextLength()
			val s = start.coerceIn(0, len)
			val e = end.coerceIn(0, len)
			if (s < e) {
				state.updateComposingRange(s, e)
			} else {
				state.clearComposingRange()
			}
		}
	}

	override fun getCursorCapsMode(reqModes: Int): Int {
		val cursorIndex = state.getCharacterIndex(state.cursorPosition)
		return TextUtils.getCapsMode(state.getAllText(), cursorIndex, reqModes)
	}

	override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText {
		// Track monitor mode. AndroidX returns a populated ExtractedText regardless of
		// whether `request` is null; some IMEs request null on initial bind.
		val monitor = (flags and InputConnection.GET_EXTRACTED_TEXT_MONITOR) != 0
		state.platformExtensions.extractedTextMonitorEnabled = monitor
		if (monitor) {
			state.platformExtensions.extractedTextMonitorToken = request?.token ?: 0
		}
		return state.toExtractedText()
	}

	@RequiresApi(Build.VERSION_CODES.S)
	override fun getSurroundingText(
		beforeLength: Int,
		afterLength: Int,
		flags: Int
	): SurroundingText {
		val cursorIndex = state.getCharacterIndex(state.cursorPosition)
		val textLength = state.getTextLength()
		val start = maxOf(0, cursorIndex - beforeLength)
		val end = minOf(textLength, cursorIndex + afterLength)
		val text = if (start < end) {
			state.getAllText().subSequence(start, end).toString()
		} else {
			""
		}
		val selection = state.selector.selection
		val selectionStart: Int
		val selectionEnd: Int
		if (selection != null) {
			val selStart = state.getCharacterIndex(selection.start)
			val selEnd = state.getCharacterIndex(selection.end)
			selectionStart = (selStart - start).coerceIn(0, text.length)
			selectionEnd = (selEnd - start).coerceIn(0, text.length)
		} else {
			selectionStart = (cursorIndex - start).coerceIn(0, text.length)
			selectionEnd = selectionStart
		}
		return SurroundingText(text, selectionStart, selectionEnd, start)
	}

	// ============ BATCH EDITS ============

	override fun beginBatchEdit(): Boolean = ensureActive {
		state.platformExtensions.beginBatchEdit()
	}

	override fun endBatchEdit(): Boolean {
		if (!isActive) return false
		val batchEnded = state.platformExtensions.endBatchEdit()
		if (batchEnded) {
			// Drain queued ops with isInBatchEdit == false so they apply directly.
			if (pendingOps.isNotEmpty()) {
				val ops = pendingOps.toList()
				pendingOps.clear()
				for (op in ops) op()
			}
			notifyImeOfCurrentState()
		}
		// Per InputConnection contract: return true if a batch is still in progress.
		return !batchEnded
	}

	private fun notifyImeOfCurrentState() {
		val view = state.platformExtensions.view ?: return
		val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE)
				as? InputMethodManager ?: return

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

		val composingRange = state.composingRange
		val candidatesStart = composingRange?.let { state.getCharacterIndex(it.start) } ?: -1
		val candidatesEnd = composingRange?.let { state.getCharacterIndex(it.end) } ?: -1

		imm.updateSelection(view, selStart, selEnd, candidatesStart, candidatesEnd)

		if (state.platformExtensions.extractedTextMonitorEnabled) {
			imm.updateExtractedText(
				view,
				state.platformExtensions.extractedTextMonitorToken,
				state.toExtractedText()
			)
		}
	}

	// ============ KEY EVENTS ============

	override fun sendKeyEvent(event: KeyEvent?): Boolean {
		if (!isActive || event == null) return false

		// Forward UP events as handled when the matching DOWN was handled by us, so the
		// system doesn't try to deliver them elsewhere.
		if (event.action != KeyEvent.ACTION_DOWN) {
			return event.action == KeyEvent.ACTION_UP &&
					isHandledKeyCode(event.keyCode)
		}

		val handled = handleKeyDown(event)
		return handled
	}

	private fun isHandledKeyCode(keyCode: Int): Boolean = when (keyCode) {
		KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
		KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
		KeyEvent.KEYCODE_MOVE_HOME, KeyEvent.KEYCODE_MOVE_END,
		KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL,
		KeyEvent.KEYCODE_ENTER -> true

		else -> false
	}

	private fun handleKeyDown(event: KeyEvent): Boolean {
		val shift = event.isShiftPressed
		return when (event.keyCode) {
			KeyEvent.KEYCODE_DPAD_LEFT -> moveCursorWithOptionalSelect(shift) { state.cursor.moveLeft() }
			KeyEvent.KEYCODE_DPAD_RIGHT -> moveCursorWithOptionalSelect(shift) { state.cursor.moveRight() }
			KeyEvent.KEYCODE_DPAD_UP -> moveCursorWithOptionalSelect(shift) { state.moveCursorUp() }
			KeyEvent.KEYCODE_DPAD_DOWN -> moveCursorWithOptionalSelect(shift) { state.moveCursorDown() }
			KeyEvent.KEYCODE_MOVE_HOME -> moveCursorWithOptionalSelect(shift) { state.cursor.moveToLineStart() }
			KeyEvent.KEYCODE_MOVE_END -> moveCursorWithOptionalSelect(shift) { state.moveCursorToLineEnd() }

			KeyEvent.KEYCODE_DEL -> {
				if (state.selector.hasSelection()) state.selector.deleteSelection()
				else state.backspaceAtCursor()
				true
			}

			KeyEvent.KEYCODE_FORWARD_DEL -> {
				if (state.selector.hasSelection()) state.selector.deleteSelection()
				else state.deleteAtCursor()
				true
			}

			KeyEvent.KEYCODE_ENTER -> {
				if (state.selector.hasSelection()) state.selector.deleteSelection()
				state.insertNewlineAtCursor()
				true
			}

			else -> {
				val unicodeChar = event.unicodeChar
				if (unicodeChar != 0) {
					if (state.selector.hasSelection()) state.selector.deleteSelection()
					state.insertStringAtCursor(Char(unicodeChar).toString())
					true
				} else {
					false
				}
			}
		}
	}

	private inline fun moveCursorWithOptionalSelect(shift: Boolean, move: () -> Unit): Boolean {
		val before: CharLineOffset = state.cursorPosition
		if (!shift) state.selector.clearSelection()
		move()
		if (shift) extendSelectionForMovement(before)
		return true
	}

	private fun extendSelectionForMovement(initialPosition: CharLineOffset) {
		val current = state.selector.selection
		when {
			current == null ->
				state.selector.updateSelection(initialPosition, state.cursorPosition)

			initialPosition == current.start ->
				state.selector.updateSelection(state.cursorPosition, current.end)

			initialPosition == current.end ->
				state.selector.updateSelection(current.start, state.cursorPosition)

			else ->
				state.selector.updateSelection(initialPosition, state.cursorPosition)
		}
	}

	// ============ EDITOR ACTION / CONTEXT MENU ============

	override fun performEditorAction(editorAction: Int): Boolean = ensureActive {
		// Multi-line field: some IMEs route Enter through here instead of
		// commitText("\n") or sendKeyEvent(KEYCODE_ENTER).
		when (editorAction) {
			EditorInfo.IME_ACTION_UNSPECIFIED,
			EditorInfo.IME_ACTION_NONE -> runOrQueue {
				if (state.selector.hasSelection()) state.selector.deleteSelection()
				state.insertNewlineAtCursor()
			}

			else -> Unit
		}
	}

	override fun performContextMenuAction(id: Int): Boolean = ensureActive {
		when (id) {
			android.R.id.selectAll -> runOrQueue {
				state.selector.selectAll()
				val len = state.getTextLength()
				state.cursor.updatePosition(state.getOffsetAtCharacter(len))
			}

			android.R.id.cut -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_CUT)
			android.R.id.copy -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_COPY)
			android.R.id.paste -> sendSynthesizedKeyEvent(KeyEvent.KEYCODE_PASTE)
			else -> Unit
		}
	}

	private fun sendSynthesizedKeyEvent(code: Int) {
		sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
		sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
	}

	// ============ MISC ============

	override fun clearMetaKeyStates(states: Int): Boolean = false

	override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
		// Per docs: return true even for unknown commands, so long as the connection is alive.
		return isActive
	}

	override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean = ensureActive {
		state.platformExtensions.cursorAnchorMonitoringEnabled =
			(cursorUpdateMode and InputConnection.CURSOR_UPDATE_MONITOR) != 0
		if (cursorUpdateMode and InputConnection.CURSOR_UPDATE_IMMEDIATE != 0) {
			state.platformExtensions.sendCursorAnchorInfo()
		}
	}

	override fun getHandler(): Handler? = null

	override fun closeConnection() {
		isActive = false
		pendingOps.clear()
		state.clearComposingRange()
		state.platformExtensions.cursorAnchorMonitoringEnabled = false
		state.platformExtensions.extractedTextMonitorEnabled = false
		state.platformExtensions.extractedTextMonitorToken = 0
		state.platformExtensions.resetBatchEdit()
	}

	override fun commitCompletion(text: CompletionInfo?): Boolean = false

	override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
		// We don't implement autocorrect highlight yet but the contract expects true
		// to mean "accepted" rather than "connection dead".
		return isActive
	}

	override fun reportFullscreenMode(enabled: Boolean): Boolean = false

	override fun commitContent(
		inputContentInfo: InputContentInfo,
		flags: Int,
		opts: Bundle?
	): Boolean = false
}

/**
 * Builds the AndroidX-shaped [ExtractedText] for the current state. Mirrors
 * `TextFieldValue.toExtractedText` from StatelessInputConnection.android.kt.
 */
internal fun TextEditorState.toExtractedText(): ExtractedText {
	val res = ExtractedText()
	val all = getAllText()
	res.text = all
	res.startOffset = 0
	res.partialStartOffset = -1 // -1 means full text
	res.partialEndOffset = all.length
	val cursorIndex = getCharacterIndex(cursorPosition)
	val selection = selector.selection
	if (selection != null) {
		res.selectionStart = getCharacterIndex(selection.start)
		res.selectionEnd = getCharacterIndex(selection.end)
	} else {
		res.selectionStart = cursorIndex
		res.selectionEnd = cursorIndex
	}
	res.flags = if ('\n' in all) 0 else ExtractedText.FLAG_SINGLE_LINE
	return res
}
