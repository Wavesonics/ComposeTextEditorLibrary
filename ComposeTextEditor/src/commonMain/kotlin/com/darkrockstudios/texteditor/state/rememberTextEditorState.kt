package com.darkrockstudios.texteditor.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun rememberTextEditorState(initialText: AnnotatedString? = null): TextEditorState {
	val scope = rememberCoroutineScope()
	val density = LocalDensity.current
	val windowInfo = LocalWindowInfo.current

	// Trigger recomposition when window info changes
	val measuringKey = remember(density, windowInfo) { Uuid.random() }
	val textMeasurer = rememberTextMeasurer()

	val state = remember {
		TextEditorState(
			scope = scope,
			measurer = textMeasurer,
			initialText = initialText,
		)
	}

	LaunchedEffect(measuringKey, textMeasurer) {
		state.textMeasurer = textMeasurer
	}

	return state
}