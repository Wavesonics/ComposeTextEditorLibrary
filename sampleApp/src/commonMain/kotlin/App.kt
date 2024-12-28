import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.TextEditor
import com.darkrockstudios.texteditor.markdown.MarkdownStyles
import com.darkrockstudios.texteditor.richstyle.HighlightSpanStyle
import com.darkrockstudios.texteditor.richstyle.RichSpan
import com.darkrockstudios.texteditor.richstyle.SpellCheckStyle
import com.darkrockstudios.texteditor.state.SpanClickType
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.getRichSpansAtPosition
import com.darkrockstudios.texteditor.state.getRichSpansInRange
import com.darkrockstudios.texteditor.state.getSpanStylesAtPosition
import com.darkrockstudios.texteditor.state.getSpanStylesInRange
import com.darkrockstudios.texteditor.state.rememberTextEditorState
import org.jetbrains.compose.ui.tooling.preview.Preview

val BOLD = MarkdownStyles.BOLD
val ITALICS = MarkdownStyles.ITALICS
val HIGHLIGHT = HighlightSpanStyle(Color(0x40FF0000))

@Composable
@Preview
fun App() {
    MaterialTheme {
        //val state: TextEditorState = rememberTextEditorState(SIMPLE_MARKDOWN.toAnnotatedStringFromMarkdown())
        val state: TextEditorState = rememberTextEditorState(createRichTextDemo())
        //val state: TextEditorState = rememberTextEditorState(createRichTextDemo2())
        //val state: TextEditorState = rememberTextEditorState(alice_wounder_land.toAnnotatedStringFromMarkdown())

        var isBoldActive by remember { mutableStateOf(false) }
        var isItalicActive by remember { mutableStateOf(false) }
        var isHighlightActive by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            state.cursorPositionFlow.collect { position ->
                val selection = state.selector.selection
                val styles = if (selection != null) {
                    state.getSpanStylesInRange(selection)
                } else {
                    state.getSpanStylesAtPosition(position)
                }

                val richSpans = if (selection != null) {
                    state.getRichSpansInRange(selection)
                } else {
                    state.getRichSpansAtPosition(position)
                }

                isBoldActive = styles.contains(BOLD)
                isItalicActive = styles.contains(ITALICS)
                isHighlightActive = richSpans.any { it.style == HIGHLIGHT }
            }
        }

        LaunchedEffect(Unit) {
//            state.selector.updateSelection(CharLineOffset(0, 10), CharLineOffset(0, 20))
            state.addRichSpan(6, 11, HIGHLIGHT)
            state.addRichSpan(16, 31, SpellCheckStyle())
//
//            state.addRichSpan(30, 35, HIGHLIGHT)

            state.editOperations.collect { operation ->
                println("Applying Operation: $operation")
            }
        }

        Column {
            Text(
                "Compose Text Editor",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextEditorToolbar(
                onBoldClick = {

                    state.selector.selection?.let { range ->
                        //state.debugSpanStyles(range)
                        if (isBoldActive) {
                            state.removeStyleSpan(range, BOLD)
                        } else {
                            state.addStyleSpan(range, BOLD)
                        }
                        //state.debugSpanStyles(range)
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
                    state.selector.selection?.let { range ->
                        if (isHighlightActive) {
                            state.removeRichSpan(range.start, range.end, HIGHLIGHT)
                        } else {
                            state.addRichSpan(range.start, range.end, HIGHLIGHT)
                        }
                    }
                    isHighlightActive = !isHighlightActive
                },
                onUndoClick = state::undo,
                onRedoClick = state::redo,
                isBoldActive = isBoldActive,
                isItalicActive = isItalicActive,
                isHighlightActive = isHighlightActive,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
            )

            TextEditor(
                state = state,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                onSpanClick = { span, clickType ->
                    when (clickType) {
                        SpanClickType.TAP -> println("Touch tap on span: $span")
                        SpanClickType.PRIMARY_CLICK -> println("Left click on span: $span")
                        SpanClickType.SECONDARY_CLICK -> println("Right click on span: $span")
                    }
                }
            )
        }
    }
}