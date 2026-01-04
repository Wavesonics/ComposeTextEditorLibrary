package com.darkrockstudios.texteditor.state

/**
 * Platform-specific extensions for TextEditorState.
 * Each platform actual adds its own platform-specific members.
 *
 * On Android: Contains IME-related functionality (cursor anchor monitoring, etc.)
 * On Desktop/WASM: Empty class (no-op)
 */
expect class PlatformTextEditorExtensions(state: TextEditorState)
