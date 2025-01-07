package com.darkrockstudios.texteditor.scrollbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.darkrockstudios.texteditor.state.TextEditorScrollState

@Composable
expect fun TextEditorScrollbar(
    modifier: Modifier = Modifier,
    scrollState: TextEditorScrollState,
    content: @Composable (modifier: Modifier) -> Unit,
)