import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.darkrockstudios.texteditor.markdown.MarkdownStyles
import com.darkrockstudios.texteditor.richstyle.HighlightSpanStyle
import org.jetbrains.compose.ui.tooling.preview.Preview

val BOLD = MarkdownStyles.BOLD
val ITALICS = MarkdownStyles.ITALICS
val HIGHLIGHT = HighlightSpanStyle(Color(0x40FF0000))

enum class Destination {
	Menu,
	TextEditor,
	SpellChecking
}

@Composable
@Preview
fun App() {
	MaterialTheme {
		var curLocation by remember { mutableStateOf(Destination.Menu) }

		fun navigateTo(destination: Destination) {
			curLocation = destination
		}

		when (curLocation) {
			Destination.Menu -> HomeMenu(
				navigateTo = ::navigateTo,
			)

			Destination.TextEditor -> TextEditorDemoUi(
				navigateTo = ::navigateTo,
			)

			Destination.SpellChecking -> SpellCheckingTextEditorDemoUi(
				navigateTo = ::navigateTo,
			)
		}
	}
}