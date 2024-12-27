import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.TextEditor
import com.darkrockstudios.texteditor.richstyle.HighlightSpanStyle
import com.darkrockstudios.texteditor.richstyle.SpellCheckStyle
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.debugRichSpans
import com.darkrockstudios.texteditor.state.getSpanStylesAtPosition
import com.darkrockstudios.texteditor.state.rememberTextEditorState
import org.jetbrains.compose.ui.tooling.preview.Preview

val BOLD = SpanStyle(fontWeight = FontWeight.Bold)
val ITALICS = SpanStyle(fontStyle = FontStyle.Italic)

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

//            state.setInitialText(createRichTextDemo2())
//            state.addRichSpan(30, 35, HighlightSpanStyle(Color(0x40FF0000)))

            state.editOperations.collect { operation ->
                println("Applying Operation: $operation")
            }
        }
        var isBoldActive by remember { mutableStateOf(false) }
        var isItalicActive by remember { mutableStateOf(false) }
        var isHighlightActive by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            state.cursorPositionFlow.collect { position ->
                val styles = state.getSpanStylesAtPosition(position)
                //val richSpans = state.getRichSpansAtPosition(position)
                isBoldActive = styles.contains(BOLD)
                isItalicActive = styles.contains(ITALICS)
            }
        }

        Column {
            Text(
                "Compose Text Editor",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold
            )
            TextEditorToolbar(
                onBoldClick = {

                    state.selector.selection?.let { range ->
                        state.debugRichSpans()
                        if (isBoldActive) {
                            state.removeStyleSpan(range, BOLD)
                        } else {
                            state.addStyleSpan(range, BOLD)
                        }
                        state.debugRichSpans()
                    }
                    isBoldActive = !isBoldActive
                },
                onItalicClick = {
                    state.selector.selection?.let { range ->
                        if (isItalicActive) {
                            state.removeStyleSpan(range, ITALICS)
                        } else {
                            state.addStyleSpan(range, ITALICS)
                        }
                    }
                    isItalicActive = !isItalicActive
                },
                onHighlightClick = {
                    isHighlightActive = !isHighlightActive
                    // Implement highlight styling logic
                },
                isBoldActive = isBoldActive,
                isItalicActive = isItalicActive,
                isHighlightActive = isHighlightActive
            )

            TextEditor(
                state = state,
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                onSpanClick = { span, clickType ->
                    when (clickType) {
                        SpanClickType.TAP -> {
                            println("Touch tap on span: $span")
                        }

                        SpanClickType.PRIMARY_CLICK -> {
                            println("Left click on span: $span")
                        }

                        SpanClickType.SECONDARY_CLICK -> {
                            println("Right click on span: $span")
                        }
                    }
                }
            )
        }
    }
}