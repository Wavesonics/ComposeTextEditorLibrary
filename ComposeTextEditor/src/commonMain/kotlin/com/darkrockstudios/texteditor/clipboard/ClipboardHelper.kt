package com.darkrockstudios.texteditor.clipboard

import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString

/**
 * Platform-specific clipboard helper for text operations.
 *
 * Since `ClipEntry` cannot be constructed from commonMain and the new Compose
 * `Clipboard` API uses suspend functions, this expect/actual pattern routes
 * each platform's native clipboard call through a single common surface.
 *
 * All platforms publish two clipboard payloads — `text/html` (the rich one,
 * carrying `SpanStyle` formatting via the converters in this package) and
 * `text/plain` (a fallback for apps that don't read HTML). On read, HTML is
 * preferred; plain text is used if no HTML payload is present.
 */
expect object ClipboardHelper {
	/**
	 * Reads text from the clipboard. Returns an [AnnotatedString] reconstructed
	 * from the clipboard's HTML payload if present, otherwise from plain text.
	 */
	suspend fun getText(clipboard: Clipboard): AnnotatedString?

	/**
	 * Writes [text] to the clipboard with both HTML (preserving spans) and
	 * plain-text representations.
	 */
	suspend fun setText(clipboard: Clipboard, text: AnnotatedString)
}
