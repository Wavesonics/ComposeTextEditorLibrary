package com.darkrockstudios.texteditor.state

import android.content.Context
import android.graphics.Matrix
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.InputMethodManager

/**
 * Android-specific extensions for TextEditorState.
 * Contains IME-related functionality for cursor anchor monitoring.
 */
actual class PlatformTextEditorExtensions actual constructor(
	private val state: TextEditorState
) {
	/**
	 * The Android View associated with this text editor instance.
	 * Used for IME operations (cursor anchor info, selection updates).
	 * Set by CaptureViewForIme composable when the editor is composed.
	 */
	internal var view: View? = null

	/**
	 * When true, cursor anchor info should be sent to the IME whenever the cursor moves.
	 * Set by [requestCursorUpdates] when IME requests CURSOR_UPDATE_MONITOR mode.
	 */
	var cursorAnchorMonitoringEnabled: Boolean = false

	/**
	 * Tracks the batch edit depth. IMEs may nest batch edits.
	 * When > 0, IME cursor sync updates should be suppressed.
	 */
	private var batchEditDepth: Int = 0

	/**
	 * Whether a batch edit is currently in progress.
	 * During batch edits, IME cursor sync updates are suppressed to avoid
	 * unnecessary intermediate updates.
	 */
	val isInBatchEdit: Boolean get() = batchEditDepth > 0

	/**
	 * Begins a batch edit. Call [endBatchEdit] when done.
	 * Batch edits can be nested.
	 */
	fun beginBatchEdit() {
		batchEditDepth++
	}

	/**
	 * Ends a batch edit started by [beginBatchEdit].
	 * @return true if all batch edits have ended (depth == 0)
	 */
	fun endBatchEdit(): Boolean {
		if (batchEditDepth > 0) {
			batchEditDepth--
		}
		return batchEditDepth == 0
	}

	/**
	 * Sends cursor anchor information to the IME.
	 * This provides the keyboard with cursor position for floating toolbars and other UI.
	 *
	 * Called:
	 * - Immediately when IME requests CURSOR_UPDATE_IMMEDIATE
	 * - On cursor changes when CURSOR_UPDATE_MONITOR is active
	 */
	fun sendCursorAnchorInfo() {
		val view = view ?: return
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
