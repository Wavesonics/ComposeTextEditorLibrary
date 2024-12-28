package com.darkrockstudios.texteditor.scrollbar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun TextEditorScrollbar(
	modifier: Modifier,
	scrollState: ScrollState,
	content: @Composable () -> Unit
) {
	val scrollAdapter = rememberScrollbarAdapter(scrollState)
	Box(
		modifier = modifier
	) {
		content()
		VerticalScrollbar(
			modifier = Modifier
				.align(Alignment.CenterEnd)
				.fillMaxHeight()
				.padding(end = 1.dp),
			adapter = scrollAdapter,
		)
	}
}