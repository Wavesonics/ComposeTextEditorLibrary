import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditor
import com.darkrockstudios.texteditor.richstyle.HighlightSpanStyle
import com.darkrockstudios.texteditor.richstyle.SpellCheckStyle
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
            //state.setInitialText(alice_wounder_land)
            state.setInitialText(createRichTextDemo())

            state.selector.updateSelection(CharLineOffset(0, 10), CharLineOffset(0, 20))
            state.addRichSpan(6, 11, HighlightSpanStyle(Color(0x40FF0000)))
            state.addRichSpan(15, 30, SpellCheckStyle())
        }
        Column {
            Text(
                "Custom Text Editor",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold
            )
            TextEditor(
                state = state,
                modifier = Modifier.padding(16.dp).fillMaxSize()
            )
        }
    }
}