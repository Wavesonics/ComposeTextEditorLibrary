@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.darkrockstudios.texteditor.clipboard

import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIPasteboard

private const val UTI_HTML = "public.html"
private const val UTI_PLAIN = "public.utf8-plain-text"

actual object ClipboardHelper {
	actual suspend fun getText(clipboard: Clipboard): AnnotatedString? {
		val pb = UIPasteboard.generalPasteboard

		// Prefer HTML if any source app published it.
		readString(pb, UTI_HTML)?.let { return it.htmlToAnnotatedString() }

		// Fall back to plain text.
		return pb.string?.let { AnnotatedString(it) }
	}

	actual suspend fun setText(clipboard: Clipboard, text: AnnotatedString) {
		val html = text.toHtml()
		val plain = text.text

		val item: Map<Any?, Any?> = mapOf(
			UTI_HTML to html,
			UTI_PLAIN to plain,
		)
		UIPasteboard.generalPasteboard.setItems(listOf(item))
	}
}

private fun readString(pb: UIPasteboard, type: String): String? {
	// valueForPasteboardType returns whatever object was put on the pasteboard
	// (often a String for text types). dataForPasteboardType always returns the
	// raw bytes — required for cross-app HTML which other apps publish as NSData.
	(pb.valueForPasteboardType(type) as? String)?.takeIf { it.isNotEmpty() }?.let { return it }
	val data = pb.dataForPasteboardType(type) ?: return null
	return data.toUtf8String()
}

private fun NSData.toUtf8String(): String? =
	NSString.create(data = this, encoding = NSUTF8StringEncoding)?.toString()
