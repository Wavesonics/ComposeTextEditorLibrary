package com.darkrockstudios.texteditor

import androidx.compose.foundation.border
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState

@Composable
fun TextEditor(
	state: TextEditorState = rememberTextEditorState(),
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	style: TextEditorStyle = rememberTextEditorStyle(),
	onRichSpanClick: RichSpanClickListener? = null,
) {
	Surface(modifier = modifier.focusBorder(state.isFocused && enabled, style)) {
		BasicTextEditor(
			state = state,
			modifier = Modifier,
			enabled = enabled,
			style = style,
			onRichSpanClick = onRichSpanClick,
		)
	}
}

private fun Modifier.focusBorder(isFocused: Boolean, style: TextEditorStyle): Modifier {
	return this.border(
		width = 1.dp,
		color = if (isFocused) style.focusedBorderColor else style.unfocusedBorderColor
	)
}