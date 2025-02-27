import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.markdown.MarkdownConfiguration
import com.darkrockstudios.texteditor.markdown.toAnnotatedStringFromMarkdown
import com.darkrockstudios.texteditor.spellcheck.SpellCheckingTextEditor
import com.darkrockstudios.texteditor.spellcheck.markdown.withMarkdown
import com.darkrockstudios.texteditor.spellcheck.rememberSpellCheckState

@Composable
fun SpellCheckingTextEditorDemoUi(
	modifier: Modifier = Modifier,
	navigateTo: (Destination) -> Unit,
	configuration: MarkdownConfiguration,
) {
	val spellChecker by rememberSampleSpellChecker()
	val state =
		rememberSpellCheckState(
			spellChecker,
			SIMPLE_MARKDOWN.toAnnotatedStringFromMarkdown(configuration)
		)
	val markdownExtension = remember(state) { state.withMarkdown(configuration) }

	Column(modifier = modifier) {
		Row {
			Text(
				"Spell Checking",
				modifier = Modifier.padding(8.dp),
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold
			)
			if (spellChecker == null) {
				CircularProgressIndicator()
			}
			Spacer(modifier = Modifier.weight(1f))
			Button(onClick = { navigateTo(Destination.Menu) }) {
				Text("X")
			}
		}

		TextEditorToolbar(
			mardkown = markdownExtension,
			markdownControls = true
		)

		SpellCheckingTextEditor(
			state = state,
			modifier = Modifier
				.padding(16.dp)
				.fillMaxSize(),
		)
	}
}