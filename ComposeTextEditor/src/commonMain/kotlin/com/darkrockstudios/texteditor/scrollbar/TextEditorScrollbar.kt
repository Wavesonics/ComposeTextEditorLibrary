package com.darkrockstudios.texteditor.scrollbar

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun TextEditorScrollbar(
	modifier: Modifier = Modifier,
	scrollState: ScrollState,
	content: @Composable () -> Unit,
)