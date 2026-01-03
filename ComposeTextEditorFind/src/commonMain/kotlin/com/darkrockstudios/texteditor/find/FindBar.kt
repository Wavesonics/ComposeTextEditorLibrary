package com.darkrockstudios.texteditor.find

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A find bar UI component that provides search and replace functionality.
 *
 * @param state The FindState managing the search
 * @param onClose Called when the user closes the find bar
 * @param modifier Modifier for the find bar
 * @param strings Localizable strings for the UI. Defaults to English.
 * @param requestFocus Whether to request focus on the search field when shown
 */
@Composable
fun FindBar(
	state: FindState,
	onClose: () -> Unit,
	modifier: Modifier = Modifier,
	strings: FindBarStrings = FindBarStrings.Default,
	requestFocus: Boolean = true
) {
	var searchText by remember { mutableStateOf(state.query) }
	var replaceText by remember { mutableStateOf("") }
	var showReplace by remember { mutableStateOf(false) }
	val focusRequester = remember { FocusRequester() }

	// Request focus when shown
	LaunchedEffect(requestFocus) {
		if (requestFocus) {
			focusRequester.requestFocus()
		}
	}

	val textColor = MaterialTheme.colorScheme.onSurface
	val textStyle = TextStyle(fontSize = 14.sp, color = textColor)
	val borderColor = MaterialTheme.colorScheme.outline
	val cursorColor = MaterialTheme.colorScheme.primary
	val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant

	Surface(
		modifier = modifier.fillMaxWidth(),
		tonalElevation = 2.dp,
		shadowElevation = 2.dp
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 8.dp)
		) {
			// First row: Find
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				// Search input with custom styling
				Box(
					modifier = Modifier
						.weight(1f)
						.border(1.dp, borderColor, RoundedCornerShape(4.dp))
						.padding(horizontal = 8.dp, vertical = 6.dp),
					contentAlignment = Alignment.CenterStart
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier.fillMaxWidth()
					) {
						Box(modifier = Modifier.weight(1f)) {
							if (searchText.isEmpty()) {
								Text(
									text = strings.placeholder,
									style = textStyle.copy(color = placeholderColor)
								)
							}
							BasicTextField(
								value = searchText,
								onValueChange = { newValue ->
									searchText = newValue
									state.search(newValue)
								},
								modifier = Modifier
									.fillMaxWidth()
									.focusRequester(focusRequester)
									.onPreviewKeyEvent { event ->
										if (event.type == KeyEventType.KeyDown) {
											when {
												event.key == Key.Enter && event.isShiftPressed -> {
													state.findPrevious()
													true
												}

												event.key == Key.Enter -> {
													state.findNext()
													true
												}

												event.key == Key.Escape -> {
													onClose()
													true
												}

												else -> false
											}
										} else false
									},
								singleLine = true,
								textStyle = textStyle,
								cursorBrush = SolidColor(cursorColor),
								keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
								keyboardActions = KeyboardActions(
									onSearch = { state.findNext() }
								)
							)
						}

						if (searchText.isNotEmpty()) {
							IconButton(
								onClick = {
									searchText = ""
									state.clearSearch()
								},
								modifier = Modifier.size(18.dp)
							) {
								Icon(
									imageVector = Icons.Default.Clear,
									contentDescription = strings.clearSearch,
									modifier = Modifier.size(14.dp)
								)
							}
						}
					}
				}

				// Match count
				if (state.query.isNotEmpty()) {
					Text(
						text = if (state.matchCount > 0) {
							strings.matchCount(state.currentMatchIndex + 1, state.matchCount)
						} else {
							strings.noMatches
						},
						style = MaterialTheme.typography.bodySmall,
						color = if (state.matchCount == 0) {
							MaterialTheme.colorScheme.error
						} else {
							MaterialTheme.colorScheme.onSurface
						}
					)
				}

				// Previous button
				TextButton(
					onClick = { state.findPrevious() },
					enabled = state.matchCount > 0,
					contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
				) {
					Text(strings.previousMatch)
				}

				// Next button
				TextButton(
					onClick = { state.findNext() },
					enabled = state.matchCount > 0,
					contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
				) {
					Text(strings.nextMatch)
				}

				// Replace toggle button
				TextButton(
					onClick = { showReplace = !showReplace },
					contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
				) {
					Text(if (showReplace) strings.hideReplace else strings.showReplace)
				}

				// Close button
				TextButton(
					onClick = {
						state.clearSearch()
						onClose()
					},
					contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
				) {
					Text(strings.close)
				}
			}

			// Second row: Replace (animated visibility)
			AnimatedVisibility(visible = showReplace) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 4.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					// Replace input with custom styling
					Box(
						modifier = Modifier
							.weight(1f)
							.border(1.dp, borderColor, RoundedCornerShape(4.dp))
							.padding(horizontal = 8.dp, vertical = 6.dp),
						contentAlignment = Alignment.CenterStart
					) {
						Row(
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier.fillMaxWidth()
						) {
							Box(modifier = Modifier.weight(1f)) {
								if (replaceText.isEmpty()) {
									Text(
										text = strings.replacePlaceholder,
										style = textStyle.copy(color = placeholderColor)
									)
								}
								BasicTextField(
									value = replaceText,
									onValueChange = { replaceText = it },
									modifier = Modifier
										.fillMaxWidth()
										.onPreviewKeyEvent { event ->
											if (event.type == KeyEventType.KeyDown) {
												when {
													event.key == Key.Enter -> {
														state.replaceCurrent(replaceText)
														true
													}

													event.key == Key.Escape -> {
														onClose()
														true
													}

													else -> false
												}
											} else false
										},
									singleLine = true,
									textStyle = textStyle,
									cursorBrush = SolidColor(cursorColor),
									keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
									keyboardActions = KeyboardActions(
										onDone = { state.replaceCurrent(replaceText) }
									)
								)
							}

							if (replaceText.isNotEmpty()) {
								IconButton(
									onClick = { replaceText = "" },
									modifier = Modifier.size(18.dp)
								) {
									Icon(
										imageVector = Icons.Default.Clear,
										contentDescription = strings.clearSearch,
										modifier = Modifier.size(14.dp)
									)
								}
							}
						}
					}

					// Replace button
					TextButton(
						onClick = { state.replaceCurrent(replaceText) },
						enabled = state.matchCount > 0,
						contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
					) {
						Text(strings.replace)
					}

					// Replace All button
					TextButton(
						onClick = { state.replaceAll(replaceText) },
						enabled = state.matchCount > 0,
						contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
					) {
						Text(strings.replaceAll)
					}
				}
			}
		}
	}
}
