import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.TextEditor
import com.darkrockstudios.texteditor.TextOffset
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val state: TextEditorState = rememberTextEditorState()
        LaunchedEffect(Unit) {
            //val text = "test ssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss\nxxxxxxxxxxxxxxxxxx\nHello cat\n".repeat(5)
            state.setInitialText(alice_wounder_land)

            state.selector.updateSelection(TextOffset(0, 10), TextOffset(0, 20))
        }
        TextEditor(
            state = state,
            modifier = Modifier.padding(16.dp).fillMaxSize()
        )
    }
}