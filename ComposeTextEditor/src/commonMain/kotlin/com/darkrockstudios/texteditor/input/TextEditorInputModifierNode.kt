package com.darkrockstudios.texteditor.input

import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Core modifier node for text input handling in the text editor.
 *
 * This node combines:
 * - KeyInputModifierNode: For handling keyboard shortcuts and commands
 * - FocusEventModifierNode: For managing focus state
 * - PlatformTextInputModifierNode: For platform-specific text input (IME) handling
 */
internal class TextEditorInputModifierNode(
	var state: TextEditorState,
	var clipboard: Clipboard,
	var enabled: Boolean
) : androidx.compose.ui.Modifier.Node(),
	KeyInputModifierNode,
	FocusEventModifierNode,
	PlatformTextInputModifierNode {

	private val keyCommandHandler = TextEditorKeyCommandHandler()
	private var inputSessionJob: Job? = null
	private var imeCursorSync: ImeCursorSync? = null

	override fun onFocusEvent(focusState: FocusState) {
		if (enabled) {
			// Only update the focus state when enabled
			state.updateFocus(focusState.isFocused)
			if (focusState.isFocused) {
				launchTextInputSession()
			} else {
				stopTextInputSession()
			}
		} else {
			// When disabled, we still want to receive keyboard events but not show as "focused"
			state.updateFocus(false)
			stopTextInputSession()
		}
	}

	private fun stopTextInputSession() {
		inputSessionJob?.cancel()
		inputSessionJob = null
		imeCursorSync?.stopSync()
		imeCursorSync = null
	}

	private fun launchTextInputSession() {
		inputSessionJob?.cancel()

		// Start IME cursor synchronization (syncs cursor/selection changes to keyboard)
		imeCursorSync?.stopSync()
		imeCursorSync = ImeCursorSync(state).also { sync ->
			sync.startSync()
		}

		inputSessionJob = coroutineScope.launch {
			establishTextInputSession {
				// Start platform-specific input method
				// On Android: Opens soft keyboard and establishes InputConnection
				// On Desktop/WASM: Suspends indefinitely (keyboard input via KEY_TYPED)
				TextEditorTextInputService(state).startInput(this)
			}
		}
	}

	override fun onPreKeyEvent(event: KeyEvent): Boolean {
		return keyCommandHandler.handleKeyEvent(event, state, clipboard, coroutineScope, enabled)
	}

	override fun onKeyEvent(event: KeyEvent): Boolean {
		if (!enabled) return false
		// Handle character input (KEY_TYPED events on desktop arrive here as Unknown type)
		return keyCommandHandler.handleCharacterInput(event, state)
	}

	fun update(
		state: TextEditorState,
		clipboard: Clipboard,
		enabled: Boolean
	) {
		this.state = state
		this.clipboard = clipboard
		this.enabled = enabled
	}
}

/**
 * ModifierNodeElement that creates and manages TextEditorInputModifierNode.
 */
internal data class TextEditorInputModifierElement(
	val state: TextEditorState,
	val clipboard: Clipboard,
	val enabled: Boolean
) : ModifierNodeElement<TextEditorInputModifierNode>() {

	override fun create(): TextEditorInputModifierNode {
		return TextEditorInputModifierNode(state, clipboard, enabled)
	}

	override fun update(node: TextEditorInputModifierNode) {
		node.update(state, clipboard, enabled)
	}

	override fun InspectorInfo.inspectableProperties() {
		name = "textEditorInput"
		properties["enabled"] = enabled
	}
}
