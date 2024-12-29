import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.getRichSpansAtPosition
import com.darkrockstudios.texteditor.state.getRichSpansInRange
import com.darkrockstudios.texteditor.state.getSpanStylesAtPosition
import com.darkrockstudios.texteditor.state.getSpanStylesInRange

@Composable
fun TextEditorToolbar(
	state: TextEditorState,
	modifier: Modifier = Modifier,
) {
	var isBoldActive by remember { mutableStateOf(false) }
	var isItalicActive by remember { mutableStateOf(false) }
	var isHighlightActive by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		state.cursorPositionFlow.collect { position ->
			val selection = state.selector.selection
			val styles = if (selection != null) {
				state.getSpanStylesInRange(selection)
			} else {
				state.getSpanStylesAtPosition(position)
			}

			val richSpans = if (selection != null) {
				state.getRichSpansInRange(selection)
			} else {
				state.getRichSpansAtPosition(position)
			}

			isBoldActive = styles.contains(BOLD)
			isItalicActive = styles.contains(ITALICS)
			isHighlightActive = richSpans.any { it.style == HIGHLIGHT }
		}
	}

	Surface(
		modifier = modifier.fillMaxWidth(),
	) {
		Row {
			IconButton(
				onClick = state::undo,
				enabled = state.canUndo
			) {
				Icon(
					imageVector = Icons.AutoMirrored.Filled.Undo,
					contentDescription = "Undo",
					tint = if (state.canUndo) {
						MaterialTheme.colorScheme.onSurface
					} else {
						MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
					}
				)
			}

			Spacer(modifier = Modifier.width(4.dp))

			IconButton(
				onClick = state::redo,
				enabled = state.canRedo
			) {
				Icon(
					imageVector = Icons.AutoMirrored.Filled.Redo,
					contentDescription = "Redo",
					tint = if (state.canRedo) {
						MaterialTheme.colorScheme.onSurface
					} else {
						MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
					}
				)
			}

			Spacer(modifier = Modifier.width(12.dp))

			IconButton(
				onClick = {
					state.selector.selection?.let { range ->
						//state.debugSpanStyles(range)
						if (isBoldActive) {
							state.removeStyleSpan(range, BOLD)
						} else {
							state.addStyleSpan(range, BOLD)
						}
						//state.debugSpanStyles(range)
						isBoldActive = !isBoldActive
					}
				}
			) {
				Icon(
					imageVector = Icons.Default.FormatBold,
					contentDescription = "Bold",
					tint = if (isBoldActive) {
						MaterialTheme.colorScheme.primary
					} else {
						MaterialTheme.colorScheme.onSurface
					}
				)
			}

			Spacer(modifier = Modifier.width(4.dp))

			IconButton(
				onClick = {
					state.selector.selection?.let { range ->
						if (isItalicActive) {
							state.removeStyleSpan(range, ITALICS)
						} else {
							state.addStyleSpan(range, ITALICS)
						}
					}
					isItalicActive = !isItalicActive
				}
			) {
				Icon(
					imageVector = Icons.Default.FormatItalic,
					contentDescription = "Italic",
					tint = if (isItalicActive) {
						MaterialTheme.colorScheme.primary
					} else {
						MaterialTheme.colorScheme.onSurface
					}
				)
			}

			Spacer(modifier = Modifier.width(4.dp))

			IconButton(
				onClick = {
					state.selector.selection?.let { range ->
						if (isHighlightActive) {
							state.removeRichSpan(range.start, range.end, HIGHLIGHT)
						} else {
							state.addRichSpan(range.start, range.end, HIGHLIGHT)
						}
					}
					isHighlightActive = !isHighlightActive
				}
			) {
				Icon(
					imageVector = Icons.Default.Highlight,
					contentDescription = "Highlight",
					tint = if (isHighlightActive) {
						MaterialTheme.colorScheme.primary
					} else {
						MaterialTheme.colorScheme.onSurface
					}
				)
			}
		}
	}
}