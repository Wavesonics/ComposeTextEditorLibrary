package com.darkrockstudios.texteditor.input

import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
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
	var clipboardManager: ClipboardManager,
	var enabled: Boolean
) : androidx.compose.ui.Modifier.Node(),
	KeyInputModifierNode,
	FocusEventModifierNode,
	PlatformTextInputModifierNode {

	private val keyCommandHandler = TextEditorKeyCommandHandler()
	private var inputSessionJob: Job? = null

	override fun onFocusEvent(focusState: FocusState) {
		state.updateFocus(focusState.isFocused)
		if (focusState.isFocused && enabled) {
			launchTextInputSession()
		} else {
			inputSessionJob?.cancel()
			inputSessionJob = null
		}
	}

	private fun launchTextInputSession() {
		inputSessionJob?.cancel()
		inputSessionJob = coroutineScope.launch {
			establishTextInputSession {
				// The session is established - wait for platform text input
				// This is a suspend function that never returns normally
				// Platform-specific input handling will happen through PlatformTextInputMethodRequest
				awaitCancellation()
			}
		}
	}

	override fun onPreKeyEvent(event: KeyEvent): Boolean {
		if (!enabled) return false
		return keyCommandHandler.handleKeyEvent(event, state, clipboardManager)
	}

	override fun onKeyEvent(event: KeyEvent): Boolean {
		if (!enabled) return false
		// Handle character input (KEY_TYPED events on desktop arrive here as Unknown type)
		return keyCommandHandler.handleCharacterInput(event, state)
	}

	fun update(
		state: TextEditorState,
		clipboardManager: ClipboardManager,
		enabled: Boolean
	) {
		this.state = state
		this.clipboardManager = clipboardManager
		this.enabled = enabled
	}
}

/**
 * ModifierNodeElement that creates and manages TextEditorInputModifierNode.
 */
internal data class TextEditorInputModifierElement(
	val state: TextEditorState,
	val clipboardManager: ClipboardManager,
	val enabled: Boolean
) : ModifierNodeElement<TextEditorInputModifierNode>() {

	override fun create(): TextEditorInputModifierNode {
		return TextEditorInputModifierNode(state, clipboardManager, enabled)
	}

	override fun update(node: TextEditorInputModifierNode) {
		node.update(state, clipboardManager, enabled)
	}

	override fun InspectorInfo.inspectableProperties() {
		name = "textEditorInput"
		properties["enabled"] = enabled
	}
}
