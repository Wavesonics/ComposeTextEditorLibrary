package com.darkrockstudios.texteditor.clipboard

import android.content.ClipData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalComposeUiApi::class)
actual object ClipboardHelper {
	actual suspend fun getText(clipboard: Clipboard): AnnotatedString? {
		val clipData = clipboard.getClipEntry()?.clipData ?: return null
		if (clipData.itemCount == 0) return null
		val item = clipData.getItemAt(0)
		val html = item.htmlText
		if (!html.isNullOrEmpty()) {
			return html.htmlToAnnotatedString()
		}
		val plain = item.text?.toString() ?: return null
		return AnnotatedString(plain)
	}

	actual suspend fun setText(clipboard: Clipboard, text: AnnotatedString) {
		val clipData = ClipData.newHtmlText("text", text.text, text.toHtml())
		clipboard.setClipEntry(clipData.toClipEntry())
	}
}
