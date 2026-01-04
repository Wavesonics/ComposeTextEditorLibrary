package com.darkrockstudios.texteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.darkrockstudios.texteditor.state.TextEditorState

internal typealias LineDecorator = DrawScope.(
	line: Int,
	offset: Offset,
	state: TextEditorState,
	style: TextEditorStyle
) -> Unit