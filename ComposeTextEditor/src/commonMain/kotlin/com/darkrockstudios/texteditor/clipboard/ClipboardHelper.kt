package com.darkrockstudios.texteditor.clipboard

import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString

/**
 * Platform-specific clipboard helper for text operations.
 *
 * Since ClipEntry cannot be constructed from commonMain and the new Clipboard API
 * uses suspend functions, this expect/actual pattern provides platform-specific
 * implementations for clipboard text operations.
 *
 * - Desktop: Preserves full AnnotatedString styling
 * - Android: Plain text only (ClipData limitation)
 * - WASM: Plain text only (web clipboard limitation)
 */
expect object ClipboardHelper {
	/**
	 * Reads text from the clipboard.
	 * On Desktop, attempts to read AnnotatedString with styling preserved.
	 * On other platforms, returns plain text as AnnotatedString.
	 */
	suspend fun getText(clipboard: Clipboard): AnnotatedString?

	/**
	 * Writes text to the clipboard.
	 * On Desktop, preserves AnnotatedString styling.
	 * On other platforms, writes plain text only.
	 */
	suspend fun setText(clipboard: Clipboard, text: AnnotatedString)
}
