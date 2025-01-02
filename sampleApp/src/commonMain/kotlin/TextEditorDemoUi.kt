import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.TextEditor
import com.darkrockstudios.texteditor.rememberTextEditorStyle
import com.darkrockstudios.texteditor.richstyle.SpellCheckStyle
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState

@Composable
fun TextEditorDemoUi(
	modifier: Modifier = Modifier,
	navigateTo: (Destination) -> Unit,
	demoContent: Boolean,
) {
	val state: TextEditorState = if (demoContent) {
		//rememberTextEditorState(SIMPLE_MARKDOWN.toAnnotatedStringFromMarkdown())
		rememberTextEditorState(createRichTextDemo())
		//rememberTextEditorState(createRichTextDemo2())
		//rememberTextEditorState(alice_wounder_land.toAnnotatedStringFromMarkdown())
	} else {
		rememberTextEditorState()
	}

	LaunchedEffect(Unit) {
		if (demoContent) {
			//state.selector.updateSelection(CharLineOffset(0, 10), CharLineOffset(0, 20))
			state.addRichSpan(6, 11, HIGHLIGHT)
			state.addRichSpan(16, 31, SpellCheckStyle)

			//state.addRichSpan(30, 35, HIGHLIGHT)
		}

		state.editOperations.collect { operation ->
			println("Applying Operation: $operation")
		}
	}

	Column(modifier = modifier) {
		Row {
			Text(
				"Compose Text Editor",
				modifier = Modifier.padding(8.dp),
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.weight(1f))
			Button(onClick = { navigateTo(Destination.Menu) }) {
				Text("X")
			}
		}

		TextEditorToolbar(
			state = state,
		)

		val style = rememberTextEditorStyle(
			placeholderText = "Enter text here",
			textColor = MaterialTheme.colorScheme.onSurface,
		)

		TextEditor(
			state = state,
			modifier = Modifier
				.padding(16.dp)
				.fillMaxSize(),
			style = style,
			onRichSpanClick = { span, clickType, _ ->
				when (clickType) {
					SpanClickType.TAP -> println("Touch tap on span: $span")
					SpanClickType.PRIMARY_CLICK -> println("Left click on span: $span")
					SpanClickType.SECONDARY_CLICK -> println("Right click on span: $span")
				}
				true
			}
		)
	}
}