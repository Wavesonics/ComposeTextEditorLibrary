import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeMenu(
	navigateTo: (Destination) -> Unit,
	isDarkMode: Boolean,
	toggleDarkMode: (Boolean) -> Unit,
) {
	Box(modifier = Modifier.fillMaxSize()) {
		DarkModeToggle(
			isDarkMode = isDarkMode,
			onToggle = toggleDarkMode,
			modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
		)

		Column(modifier = Modifier.widthIn(max = 512.dp).align(alignment = Alignment.Center)) {
			Button(onClick = { navigateTo(Destination.TextEditor) }) {
				Text("Rich Text Editor")
			}

			Button(onClick = { navigateTo(Destination.MarkdownEditor) }) {
				Text("Markdown Text Editor")
			}

			Button(onClick = { navigateTo(Destination.EmptyTextEditor) }) {
				Text("Markdown Editor (Blank)")
			}

			Button(onClick = { navigateTo(Destination.SpellChecking) }) {
				Text("Spell Check")
			}

			Button(onClick = { navigateTo(Destination.CodeEditor) }) {
				Text("Code Editor")
			}

			Button(onClick = { navigateTo(Destination.FindDemo) }) {
				Text("Find Demo (Ctrl+F)")
			}
		}
	}
}

@Composable
fun DarkModeToggle(
	isDarkMode: Boolean,
	onToggle: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
) {
	FilledIconToggleButton(
		checked = isDarkMode,
		onCheckedChange = onToggle,
		modifier = modifier.size(40.dp),
		colors = IconButtonDefaults.filledIconToggleButtonColors(
			containerColor = MaterialTheme.colorScheme.surfaceVariant,
			contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
			checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
			checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
		)
	) {
		Icon(
			imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
			contentDescription = if (isDarkMode) "Switch to light mode" else "Switch to dark mode",
			modifier = Modifier.size(24.dp)
		)
	}
}
