package com.darkrockstudios.texteditor

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState

@Composable
fun TextEditor(
	style: TextEditorStyle = rememberTextEditorStyle(),
	state: TextEditorState = rememberTextEditorState(style),
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	onRichSpanClick: RichSpanClickListener? = null,
) {
	Surface(modifier = modifier.focusBorder(state.isFocused && enabled, style)) {
		BasicTextEditor(
			style = style,
			state = state,
			modifier = Modifier.padding(start = 8.dp),
			enabled = enabled,
			onRichSpanClick = onRichSpanClick,
		)
	}
}
