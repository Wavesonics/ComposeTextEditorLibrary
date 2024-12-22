import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TextEditorToolbar(
	onBoldClick: () -> Unit,
	onItalicClick: () -> Unit,
	onHighlightClick: () -> Unit,
	isBoldActive: Boolean = false,
	isItalicActive: Boolean = false,
	isHighlightActive: Boolean = false,
	modifier: Modifier = Modifier,
) {
	Surface(
		modifier = modifier.fillMaxWidth(),
	) {
		Row {
			IconButton(
				onClick = onBoldClick
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
				onClick = onItalicClick
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
				onClick = onHighlightClick
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