import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.RichTextView
import com.darkrockstudios.texteditor.markdown.MarkdownConfiguration
import com.darkrockstudios.texteditor.markdown.toAnnotatedStringFromMarkdown
import com.darkrockstudios.texteditor.state.rememberTextEditorState

@Composable
fun RichTextViewDemoUi(
	modifier: Modifier = Modifier,
	navigateTo: (Destination) -> Unit,
	configuration: MarkdownConfiguration,
) {
	val singleState = rememberTextEditorState(SIMPLE_MARKDOWN.toAnnotatedStringFromMarkdown(configuration))
	val cardSamples = rememberCardSamples(configuration)

	Column(modifier = modifier.fillMaxSize()) {
		Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
			Text(
				"RichTextView Demo",
				modifier = Modifier.padding(8.dp),
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
			)
			Spacer(modifier = Modifier.weight(1f))
			Button(onClick = { navigateTo(Destination.Menu) }) { Text("X") }
		}

		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(24.dp),
		) {
			Text("Plain RichTextView (no chrome, wraps to content):", style = MaterialTheme.typography.titleMedium)
			RichTextView(
				state = singleState,
				modifier = Modifier.fillMaxWidth(),
			)

			HorizontalDivider()

			Text("Inside list-style cards:", style = MaterialTheme.typography.titleMedium)
			cardSamples.forEach { state ->
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.border(
							width = 1.dp,
							color = MaterialTheme.colorScheme.outlineVariant,
							shape = RoundedCornerShape(8.dp),
						)
						.background(
							color = MaterialTheme.colorScheme.surfaceContainerLow,
							shape = RoundedCornerShape(8.dp),
						)
						.padding(12.dp),
				) {
					RichTextView(state = state)
				}
			}
		}
	}
}

@Composable
private fun rememberCardSamples(configuration: MarkdownConfiguration) = listOf(
	rememberTextEditorState(
		"# Quick note\n\nA short **bold** opener with *italic* aside and a [link](https://example.com)."
			.toAnnotatedStringFromMarkdown(configuration)
	),
	rememberTextEditorState(
		"## Meeting recap\n\nDiscussed ~~old approach~~ and the new plan. Action items follow."
			.toAnnotatedStringFromMarkdown(configuration)
	),
	rememberTextEditorState(
		"Plain paragraph with no markdown markers — should render as body text only."
			.toAnnotatedStringFromMarkdown(configuration)
	),
)
