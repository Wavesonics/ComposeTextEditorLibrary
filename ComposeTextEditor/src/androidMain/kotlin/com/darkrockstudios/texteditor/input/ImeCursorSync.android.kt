package com.darkrockstudios.texteditor.input

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditorRange
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine

/**
 * Android implementation of IME cursor synchronization.
 * Observes cursor and selection changes in TextEditorState and notifies
 * the InputMethodManager to keep the keyboard in sync.
 */
actual class ImeCursorSync actual constructor(
	private val state: TextEditorState
) {
	private var syncScope: CoroutineScope? = null
	private var viewProvider: (() -> Any?)? = null
	private var lastSelStart = -1
	private var lastSelEnd = -1

	actual fun startSync(viewProvider: () -> Any?) {
		this.viewProvider = viewProvider
		stopSync() // Cancel any existing sync

		syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
		syncScope?.launch {
			// Combine cursor position and selection flows
			combine<CharLineOffset, TextEditorRange?, Pair<CharLineOffset, TextEditorRange?>>(
				state.cursor.positionFlow,
				state.selector.selectionRangeFlow
			) { cursorPos, selection ->
				Pair(cursorPos, selection)
			}.collect { pair ->
				val view = viewProvider() as? View ?: return@collect
				val cursorPos = pair.first
				val selection = pair.second

				val selStart: Int
				val selEnd: Int

				if (selection != null) {
					selStart = state.getCharacterIndex(selection.start)
					selEnd = state.getCharacterIndex(selection.end)
				} else {
					val cursorIndex = state.getCharacterIndex(cursorPos)
					selStart = cursorIndex
					selEnd = cursorIndex
				}

				// Only update if selection actually changed
				if (selStart != lastSelStart || selEnd != lastSelEnd) {
					lastSelStart = selStart
					lastSelEnd = selEnd
					updateImeSelection(view, selStart, selEnd)
				}
			}
		}
	}

	actual fun stopSync() {
		syncScope?.cancel()
		syncScope = null
		lastSelStart = -1
		lastSelEnd = -1
	}

	private fun updateImeSelection(view: View, selStart: Int, selEnd: Int) {
		val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
			?: return

		// updateSelection parameters:
		// selStart, selEnd: current selection (same value = cursor position)
		// candidatesStart, candidatesEnd: composing region (-1 if none)
		imm.updateSelection(view, selStart, selEnd, -1, -1)
	}
}
