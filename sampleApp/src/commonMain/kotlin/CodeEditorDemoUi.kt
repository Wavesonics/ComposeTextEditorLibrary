import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.codeeditor.CodeEditor
import com.darkrockstudios.texteditor.codeeditor.rememberCodeEditorStyle
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState

@Composable
fun CodeEditorDemoUi(
	modifier: Modifier = Modifier,
	navigateTo: (Destination) -> Unit,
) {
	val state: TextEditorState = rememberTextEditorState(AnnotatedString(SAMPLE_CODE))

	LaunchedEffect(Unit) {
		state.editOperations.collect { operation ->
			println("Applying Operation: $operation")
		}
	}

	Column(modifier = modifier) {
		Row {
			Text(
				"Code Editor",
				modifier = Modifier.padding(8.dp),
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.weight(1f))
			Button(onClick = { navigateTo(Destination.Menu) }) {
				Text("X")
			}
		}

		val style = rememberCodeEditorStyle(
			placeholderText = "Enter code here",
		)

		CodeEditor(
			state = state,
			modifier = Modifier.fillMaxSize(),
			style = style,
		)
	}
}

private val SAMPLE_CODE = """
fun main() {
    println("Hello, Code Editor!")

    // This is a sample code
    for (i in 1..10) {
        println("Count: ${'$'}i")
    }

    val message = "Welcome to the Code Editor"
    println(message)
}
""".trim()
