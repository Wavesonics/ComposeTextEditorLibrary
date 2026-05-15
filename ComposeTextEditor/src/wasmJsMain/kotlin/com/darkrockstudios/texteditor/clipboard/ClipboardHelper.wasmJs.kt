@file:OptIn(ExperimentalWasmJsInterop::class)

package com.darkrockstudios.texteditor.clipboard

import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.await
import kotlin.js.Promise

actual object ClipboardHelper {
	actual suspend fun getText(clipboard: Clipboard): AnnotatedString? {
		// Try the rich (HTML+plain) Async Clipboard API first; fall back to readText
		// if the browser doesn't support read() / ClipboardItem (older Firefox, etc.).
		return try {
			val tagged = readClipboardRich().await<JsString>().toString()
			when {
				tagged.startsWith("H") -> tagged.substring(1).htmlToAnnotatedString()
				tagged.startsWith("P") -> AnnotatedString(tagged.substring(1))
				else -> null
			}
		} catch (e: Throwable) {
			try {
				val text = readClipboardText().await<JsString>().toString()
				AnnotatedString(text)
			} catch (e: Throwable) {
				null
			}
		}
	}

	actual suspend fun setText(clipboard: Clipboard, text: AnnotatedString) {
		val html = text.toHtml()
		val plain = text.text
		try {
			writeClipboardRich(html, plain).await<JsAny?>()
		} catch (e: Throwable) {
			// Fallback: writeText (plain only). Some browsers / contexts don't
			// allow ClipboardItem (Firefox without dom.events.asyncClipboard.clipboardItem).
			try {
				writeClipboardText(plain).await<JsAny?>()
			} catch (e: Throwable) {
				// Clipboard access denied — give up silently, matching prior behavior.
			}
		}
	}
}

// Returns a string tagged with one character: 'H' for HTML, 'P' for plain, '' for empty.
// Single-character prefix avoids needing a JS object bridge.
private fun readClipboardRich(): Promise<JsString> = js(
	"""
	(async () => {
		const items = await navigator.clipboard.read();
		if (!items || items.length === 0) return '';
		const item = items[0];
		if (item.types.includes('text/html')) {
			const blob = await item.getType('text/html');
			return 'H' + (await blob.text());
		}
		if (item.types.includes('text/plain')) {
			const blob = await item.getType('text/plain');
			return 'P' + (await blob.text());
		}
		return '';
	})()
	"""
)

private fun writeClipboardRich(html: String, plain: String): Promise<JsAny?> = js(
	"""
	navigator.clipboard.write([
		new ClipboardItem({
			'text/html': new Blob([html], {type: 'text/html'}),
			'text/plain': new Blob([plain], {type: 'text/plain'})
		})
	])
	"""
)

private fun readClipboardText(): Promise<JsString> = js("navigator.clipboard.readText()")

private fun writeClipboardText(text: String): Promise<JsAny?> = js("navigator.clipboard.writeText(text)")
