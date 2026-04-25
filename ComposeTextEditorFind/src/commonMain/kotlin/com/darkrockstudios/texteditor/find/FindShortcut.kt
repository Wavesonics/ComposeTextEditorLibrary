package com.darkrockstudios.texteditor.find

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Intercepts the standard find shortcut (Ctrl+F on Win/Linux, Cmd+F on macOS)
 * and invokes [onToggle] when triggered. Repeated presses toggle the find bar
 * — the consumer's [onToggle] callback is expected to flip its own visibility
 * state.
 *
 * Apply this directly to the `TextEditor`'s modifier so that the handler is on
 * the focusable node — placing it on a non-focusable ancestor does not reliably
 * receive hardware-keyboard events on Android.
 *
 * Two interception points are wired up:
 * - `onInterceptKeyBeforeSoftKeyboard` catches the event before the Android IME
 *   gets to consume it, which is required for hardware/Bluetooth keyboards while
 *   the soft keyboard is active.
 * - `onPreviewKeyEvent` is the standard Compose key dispatch path used on
 *   Desktop and as a fallback on Android when no IME is in play.
 *
 * Both handlers consume the event so the editor doesn't see Ctrl+F as a typed
 * character.
 */
fun Modifier.findShortcut(onToggle: () -> Unit): Modifier = this
	.onInterceptKeyBeforeSoftKeyboard { event -> handleFindShortcut(event, onToggle) }
	.onPreviewKeyEvent { event -> handleFindShortcut(event, onToggle) }

private fun handleFindShortcut(event: KeyEvent, onToggle: () -> Unit): Boolean {
	if (event.type == KeyEventType.KeyDown
		&& event.key == Key.F
		&& (event.isCtrlPressed || event.isMetaPressed)
	) {
		onToggle()
		return true
	}
	return false
}
