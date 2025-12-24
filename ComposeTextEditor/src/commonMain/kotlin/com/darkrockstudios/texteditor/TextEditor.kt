package com.darkrockstudios.texteditor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState

private val DefaultContentPadding = PaddingValues(start = 8.dp)

@Composable
fun TextEditor(
	state: TextEditorState = rememberTextEditorState(),
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = DefaultContentPadding,
	enabled: Boolean = true,
	autoFocus: Boolean = false,
	style: TextEditorStyle = rememberTextEditorStyle(),
	onRichSpanClick: RichSpanClickListener? = null,
) {
	Surface(modifier = modifier.focusBorder(state.isFocused && enabled, style)) {
		BasicTextEditor(
			state = state,
			modifier = Modifier.padding(contentPadding),
			enabled = enabled,
			autoFocus = autoFocus,
			style = style,
			onRichSpanClick = onRichSpanClick,
		)
	}
}
