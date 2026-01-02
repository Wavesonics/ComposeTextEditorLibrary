package com.darkrockstudios.texteditor.state

/**
 * WASM-specific extensions for TextEditorState.
 * Empty implementation - WASM doesn't have IME-specific requirements.
 */
@Suppress("UNUSED_PARAMETER")
actual class PlatformTextEditorExtensions actual constructor(
	state: TextEditorState
)
