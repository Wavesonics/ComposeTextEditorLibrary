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
	private var lastSelStart = -1
	private var lastSelEnd = -1

	actual fun startSync() {
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
				val view = state.platformExtensions.view ?: return@collect
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

					// Send cursor anchor info if monitoring is enabled
					if (state.platformExtensions.cursorAnchorMonitoringEnabled) {
						state.platformExtensions.sendCursorAnchorInfo()
					}
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

		// Get composing region if active
		val composingRange = state.composingRange
		val candidatesStart: Int
		val candidatesEnd: Int
		if (composingRange != null) {
			candidatesStart = state.getCharacterIndex(composingRange.start)
			candidatesEnd = state.getCharacterIndex(composingRange.end)
		} else {
			candidatesStart = -1
			candidatesEnd = -1
		}

		// updateSelection parameters:
		// selStart, selEnd: current selection (same value = cursor position)
		// candidatesStart, candidatesEnd: composing region (-1 if none)
		imm.updateSelection(view, selStart, selEnd, candidatesStart, candidatesEnd)
	}
}
