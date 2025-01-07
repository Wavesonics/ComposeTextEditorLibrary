package com.darkrockstudios.texteditor.scrollbar

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.darkrockstudios.texteditor.state.TextEditorScrollState

@Composable
actual fun TextEditorScrollbar(
	modifier: Modifier,
	scrollState: TextEditorScrollState,
	content: @Composable (modifier: Modifier) -> Unit
) {
	//val scrollAdapter = rememberScrollbarAdapter(scrollState)
	Box(
		modifier = modifier
	) {
		content()
//		VerticalScrollbar(
//			modifier = Modifier
//				.align(Alignment.CenterEnd)
//				.fillMaxHeight()
//				.padding(end = 1.dp),
//			adapter = scrollAdapter,
//		)
	}
}