@file:OptIn(ExperimentalTestApi::class)

package e2e

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.darkrockstudios.texteditor.RichTextView
import com.darkrockstudios.texteditor.rememberTextEditorStyle
import com.darkrockstudios.texteditor.state.TextEditorState
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextViewDecorationColorsE2eTest {
	@Test
	fun `a read-only view hands the style's decoration colors to its state`() =
		runSkikoComposeUiTest(density = Density(1f)) {
			val state = TextEditorState(AnnotatedString("1. one"))
			var marker by mutableStateOf(Color.Red)
			setContent {
				RichTextView(
					state = state,
					modifier = Modifier.size(300.dp, 200.dp),
					style = rememberTextEditorStyle(
						bulletColor = Color.Green,
						blockquoteBarColor = Color.Blue,
						blockquoteBackgroundColor = Color.Cyan,
						orderedListMarkerColor = marker,
						codeFenceBackgroundColor = Color.Magenta,
						codeFenceBorderColor = Color.Yellow,
						tableBorderColor = Color.Gray,
						tableHeaderBackgroundColor = Color.DarkGray,
					),
				)
			}
			waitForIdle()
			assertEquals(Color.Green, state.bulletColor)
			assertEquals(Color.Blue, state.blockquoteBarColor)
			assertEquals(Color.Cyan, state.blockquoteBackgroundColor)
			assertEquals(Color.Red, state.orderedListMarkerColor)
			assertEquals(Color.Magenta, state.codeFenceBackgroundColor)
			assertEquals(Color.Yellow, state.codeFenceBorderColor)
			assertEquals(Color.Gray, state.tableBorderColor)
			assertEquals(Color.DarkGray, state.tableHeaderBackgroundColor)

			marker = Color.White
			waitForIdle()
			assertEquals(Color.White, state.orderedListMarkerColor)
		}
}
