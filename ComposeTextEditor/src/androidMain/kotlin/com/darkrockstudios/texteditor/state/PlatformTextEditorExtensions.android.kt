package com.darkrockstudios.texteditor.state

import android.content.Context
import android.graphics.Matrix
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.InputMethodManager
import com.darkrockstudios.texteditor.input.AndroidViewHolder

/**
 * Android-specific extensions for TextEditorState.
 * Contains IME-related functionality for cursor anchor monitoring.
 */
actual class PlatformTextEditorExtensions actual constructor(
	private val state: TextEditorState
) {
	/**
	 * When true, cursor anchor info should be sent to the IME whenever the cursor moves.
	 * Set by [requestCursorUpdates] when IME requests CURSOR_UPDATE_MONITOR mode.
	 */
	var cursorAnchorMonitoringEnabled: Boolean = false

	/**
	 * Sends cursor anchor information to the IME.
	 * This provides the keyboard with cursor position for floating toolbars and other UI.
	 *
	 * Called:
	 * - Immediately when IME requests CURSOR_UPDATE_IMMEDIATE
	 * - On cursor changes when CURSOR_UPDATE_MONITOR is active
	 */
	fun sendCursorAnchorInfo() {
		val view = AndroidViewHolder.currentView ?: return
		val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE)
				as? InputMethodManager ?: return

		val builder = CursorAnchorInfo.Builder()

		// Set selection range
		val cursorIndex = state.getCharacterIndex(state.cursorPosition)
		val selection = state.selector.selection
		if (selection != null) {
			builder.setSelectionRange(
				state.getCharacterIndex(selection.start),
				state.getCharacterIndex(selection.end)
			)
		} else {
			builder.setSelectionRange(cursorIndex, cursorIndex)
		}

		// Set composing text info if present
		val composingRange = state.composingRange
		if (composingRange != null) {
			val composingStart = state.getCharacterIndex(composingRange.start)
			val composingEnd = state.getCharacterIndex(composingRange.end)
			if (composingStart < composingEnd && composingEnd <= state.getTextLength()) {
				builder.setComposingText(
					composingStart,
					state.getAllText().subSequence(composingStart, composingEnd)
				)
			}
		}

		// Set the transformation matrix to convert from view coordinates to screen coordinates
		val matrix = Matrix()
		val location = IntArray(2)
		view.getLocationOnScreen(location)
		matrix.setTranslate(location[0].toFloat(), location[1].toFloat())
		builder.setMatrix(matrix)

		// Set insertion marker location if we have cursor metrics
		state.lastCursorMetrics?.let { metrics ->
			builder.setInsertionMarkerLocation(
				metrics.position.x,
				metrics.lineTop,
				metrics.lineBaseline,
				metrics.lineBottom,
				0 // flags: 0 = visible
			)
		}

		try {
			imm.updateCursorAnchorInfo(view, builder.build())
		} catch (e: Exception) {
			// Ignore errors - some fields may be required on certain API levels
		}
	}
}
