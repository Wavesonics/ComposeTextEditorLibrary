package com.darkrockstudios.texteditor.state

/**
 * iOS-specific extensions for TextEditorState.
 * Empty implementation - iOS doesn't have Android-style IME requirements.
 * Compose Multiplatform handles iOS keyboard integration internally.
 */
@Suppress("UNUSED_PARAMETER")
actual class PlatformTextEditorExtensions actual constructor(
	state: TextEditorState
)
