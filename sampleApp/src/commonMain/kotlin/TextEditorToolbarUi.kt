import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.markdown.MarkdownExtension
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.getRichSpansAtPosition
import com.darkrockstudios.texteditor.state.getRichSpansInRange
import com.darkrockstudios.texteditor.state.getSpanStylesInRange
import markdown.decreaseFontSize
import markdown.increaseFontSize

@Composable
fun TextEditorToolbar(
	mardkown: MarkdownExtension,
	markdownControls: Boolean,
	modifier: Modifier = Modifier,
) {
	val state = remember(mardkown) { mardkown.editorState }

	var isBoldActive by remember { mutableStateOf(false) }
	var isItalicActive by remember { mutableStateOf(false) }
	var isCodeActive by remember { mutableStateOf(false) }
	var isLinkActive by remember { mutableStateOf(false) }
	var isBlockquoteActive by remember { mutableStateOf(false) }
	var currentHeaderLevel by remember { mutableStateOf(0) }
	var isHighlightActive by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		state.cursorDataFlow.collect { (position, cursorStyles, selection) ->
			val styles = if (selection != null) {
				state.getSpanStylesInRange(selection)
			} else {
				cursorStyles
			}

			val richSpans = if (selection != null) {
				state.getRichSpansInRange(selection)
			} else {
				state.getRichSpansAtPosition(position)
			}

			isBoldActive = styles.contains(mardkown.markdownStyles.BOLD)
			isItalicActive = styles.contains(mardkown.markdownStyles.ITALICS)
			isCodeActive = styles.contains(mardkown.markdownStyles.CODE)
			isLinkActive = styles.contains(mardkown.markdownStyles.LINK)
			isBlockquoteActive = styles.contains(mardkown.markdownStyles.BLOCKQUOTE)
			currentHeaderLevel = (1..6).firstOrNull { lvl ->
				styles.contains(mardkown.markdownStyles.header(lvl))
			} ?: 0
			isHighlightActive = richSpans.any { it.style == HIGHLIGHT }
		}
	}

	Surface(
		modifier = modifier.fillMaxWidth(),
		//color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
		//tonalElevation = 2.dp,
	) {
		Row(
			modifier = Modifier
				.horizontalScroll(rememberScrollState())
				.padding(horizontal = 16.dp, vertical = 2.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// History Controls Group
			Row {
				ToolbarButton(
					onClick = state::undo,
					icon = Icons.AutoMirrored.Filled.Undo,
					contentDescription = "Undo",
					enabled = state.canUndo
				)

				Spacer(modifier = Modifier.width(4.dp))

				ToolbarButton(
					onClick = state::redo,
					icon = Icons.AutoMirrored.Filled.Redo,
					contentDescription = "Redo",
					enabled = state.canRedo
				)
			}

			Spacer(modifier = Modifier.width(12.dp))

			VerticalDivider(modifier = Modifier.height(24.dp))

			Spacer(modifier = Modifier.width(12.dp))

			// Formatting Controls Group
			Row {
				FormatButton(
					onClick = {
						toggleStyle(state, isBoldActive, mardkown.markdownStyles.BOLD)
					},
					icon = Icons.Default.FormatBold,
					contentDescription = "Bold",
					isActive = isBoldActive,
				)

				Spacer(modifier = Modifier.width(4.dp))

				FormatButton(
					onClick = {
						toggleStyle(state, isItalicActive, mardkown.markdownStyles.ITALICS)
					},
					icon = Icons.Default.FormatItalic,
					contentDescription = "Italic",
					isActive = isItalicActive,
				)

				if (markdownControls) {
					Spacer(modifier = Modifier.width(4.dp))

					FormatButton(
						onClick = {
							toggleStyle(state, isCodeActive, mardkown.markdownStyles.CODE)
						},
						icon = Icons.Default.Code,
						contentDescription = "Inline Code",
						isActive = isCodeActive,
					)

					Spacer(modifier = Modifier.width(4.dp))

					FormatButton(
						onClick = {
							state.selector.selection?.let { range ->
								if (isLinkActive) {
									state.removeStyleSpan(range, mardkown.markdownStyles.LINK)
								} else {
									state.addStyleSpan(range, mardkown.markdownStyles.LINK)
								}
							}
						},
						icon = Icons.Default.Link,
						contentDescription = "Link",
						isActive = isLinkActive,
						enabled = state.selector.hasSelection()
					)

					Spacer(modifier = Modifier.width(4.dp))

					TextLabelButton(
						onClick = {
							cycleHeader(state, mardkown, currentHeaderLevel)
						},
						label = if (currentHeaderLevel == 0) "H" else "H$currentHeaderLevel",
						contentDescription = if (currentHeaderLevel == 0)
							"Header (none) — click to cycle"
						else
							"Header H$currentHeaderLevel — click to cycle",
						isActive = currentHeaderLevel != 0,
					)

					Spacer(modifier = Modifier.width(4.dp))

					FormatButton(
						onClick = {
							toggleStyle(
								state,
								isBlockquoteActive,
								mardkown.markdownStyles.BLOCKQUOTE
							)
						},
						icon = Icons.Default.FormatQuote,
						contentDescription = "Blockquote (style only)",
						isActive = isBlockquoteActive,
					)

					Spacer(modifier = Modifier.width(12.dp))

					// Font size control group
					VerticalDivider(modifier = Modifier.height(24.dp))

					Spacer(modifier = Modifier.width(12.dp))

					// Font size decrease button
					ToolbarButton(
						onClick = { decreaseFontSize(mardkown) },
						icon = Icons.Default.Remove,
						contentDescription = "Decrease Font Size"
					)

					Icon(
						imageVector = Icons.Default.FormatSize,
						contentDescription = null,
						modifier = Modifier
							.size(20.dp)
							.padding(horizontal = 4.dp),
						tint = MaterialTheme.colorScheme.onSurfaceVariant
					)

					// Font size increase button
					ToolbarButton(
						onClick = { increaseFontSize(mardkown) },
						icon = Icons.Default.Add,
						contentDescription = "Increase Font Size"
					)
				} else {
					Spacer(modifier = Modifier.width(4.dp))

					FormatButton(
						onClick = {
							state.selector.selection?.let { range ->
								if (isHighlightActive) {
									state.removeRichSpan(range.start, range.end, HIGHLIGHT)
								} else {
									state.addRichSpan(range.start, range.end, HIGHLIGHT)
								}
							}
						},
						icon = Icons.Default.Highlight,
						contentDescription = "Highlight",
						isActive = isHighlightActive,
						enabled = state.selector.hasSelection()
					)
				}
			}
		}
	}
}

private fun toggleStyle(
	state: TextEditorState,
	isActive: Boolean,
	spanStyle: SpanStyle
) {
	val selection = state.selector.selection
	if (selection != null) {
		if (isActive) {
			state.removeStyleSpan(selection, spanStyle)
		} else {
			state.addStyleSpan(selection, spanStyle)
		}
	} else {
		if (isActive) {
			state.cursor.removeStyle(spanStyle)
		} else {
			state.cursor.addStyle(spanStyle)
		}
	}
}

private fun cycleHeader(
	state: TextEditorState,
	markdown: MarkdownExtension,
	currentLevel: Int,
) {
	val nextLevel = (currentLevel + 1) % 7
	val selection = state.selector.selection
	if (selection != null) {
		(1..6).forEach { lvl ->
			state.removeStyleSpan(selection, markdown.markdownStyles.header(lvl))
		}
		if (nextLevel != 0) {
			state.addStyleSpan(selection, markdown.markdownStyles.header(nextLevel))
		}
	} else {
		(1..6).forEach { lvl ->
			state.cursor.removeStyle(markdown.markdownStyles.header(lvl))
		}
		if (nextLevel != 0) {
			state.cursor.addStyle(markdown.markdownStyles.header(nextLevel))
		}
	}
}

@Composable
private fun ToolbarButton(
	onClick: () -> Unit,
	icon: ImageVector,
	contentDescription: String,
	isActive: Boolean = false,
	enabled: Boolean = true,
	modifier: Modifier = Modifier
) {
	FilledTonalIconButton(
		onClick = onClick,
		enabled = enabled,
		modifier = modifier
			.size(32.dp)
			.focusable(false)
			.focusProperties {
				canFocus = false
			},
		colors = IconButtonDefaults.filledTonalIconButtonColors(
			containerColor = if (isActive)
				MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
			else
				MaterialTheme.colorScheme.surfaceVariant,
			contentColor = if (isActive)
				MaterialTheme.colorScheme.primary
			else
				MaterialTheme.colorScheme.onSurfaceVariant,
			disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
			disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
		)
	) {
		Icon(
			imageVector = icon,
			contentDescription = contentDescription,
			modifier = Modifier.size(20.dp)
		)
	}
}

@Composable
private fun FormatButton(
	onClick: () -> Unit,
	icon: ImageVector,
	contentDescription: String,
	isActive: Boolean,
	enabled: Boolean = true
) {
	ToolbarButton(
		onClick = onClick,
		icon = icon,
		contentDescription = contentDescription,
		isActive = isActive,
		enabled = enabled
	)
}

@Composable
private fun TextLabelButton(
	onClick: () -> Unit,
	label: String,
	contentDescription: String,
	isActive: Boolean,
	enabled: Boolean = true,
) {
	FilledTonalButton(
		onClick = onClick,
		enabled = enabled,
		modifier = Modifier
			.height(32.dp)
			.focusable(false)
			.focusProperties { canFocus = false },
		contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
		colors = ButtonDefaults.filledTonalButtonColors(
			containerColor = if (isActive)
				MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
			else
				MaterialTheme.colorScheme.surfaceVariant,
			contentColor = if (isActive)
				MaterialTheme.colorScheme.primary
			else
				MaterialTheme.colorScheme.onSurfaceVariant,
		)
	) {
		Text(
			text = label,
			style = MaterialTheme.typography.labelLarge,
		)
	}
}
