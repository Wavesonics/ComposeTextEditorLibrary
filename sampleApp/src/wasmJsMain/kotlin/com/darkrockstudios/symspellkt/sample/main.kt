package com.darkrockstudios.texteditor.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
	ComposeViewport(document.getElementById("ComposeTarget") as HTMLElement) {
		App()
	}
}