package blocks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import com.darkrockstudios.texteditor.BasicTextEditor
import com.darkrockstudios.texteditor.richstyle.TaskSpanStyle
import com.darkrockstudios.texteditor.state.TextEditorState
import com.darkrockstudios.texteditor.state.rememberTextEditorState
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import utils.setBlockLines
import kotlin.test.assertEquals

/**
 * The icon an editor sets over a task's box. The UI test harness drops pointer icons, so
 * this runs the editor in a bare scene whose platform records the last one set.
 */
@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
class TaskPointerIconTest {

	private class IconRecorder : PlatformContext.Empty() {
		var icon: PointerIcon? = null
		override fun setPointerIcon(pointerIcon: PointerIcon) {
			icon = pointerIcon
		}
	}

	/** The icon a mouse over the box of `- [ ] first`, then over its text, leaves set. */
	private fun iconsOverTask(
		readOnly: Boolean = false,
		enabled: Boolean = true,
		modifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(),
	): Pair<PointerIcon?, PointerIcon?> {
		val recorder = IconRecorder()
		val frames = FrameRecomposer(Dispatchers.Unconfined)
		val scene = CanvasLayersComposeScene(frames, size = IntSize(400, 200), platformContext = recorder)
		var frame = 0L
		fun settle() = repeat(3) {
			frames.performFrame(frame++ * 16_000_000L)
			scene.measureAndLayout()
		}
		try {
			lateinit var state: TextEditorState
			scene.setContent {
				state = rememberTextEditorState(initialText = AnnotatedString(""))
				BasicTextEditor(state, Modifier.fillMaxSize(), enabled = enabled, readOnly = readOnly)
			}
			settle()
			state.setBlockLines("- [ ] first")
			settle()
			val row = state.lineOffsets.first { it.line == 0 }
			val (topLeft, size) = TaskSpanStyle.checkboxBounds(row.textLayoutResult, state.density!!)
			val box = row.offset + topLeft + Offset(size.width / 2f, size.height / 2f)
			val text = row.offset + Offset(row.textLayoutResult.size.width - 4f, size.height / 2f)

			fun hover(at: Offset): PointerIcon? {
				scene.sendPointerEvent(PointerEventType.Move, at, keyboardModifiers = modifiers)
				settle()
				return recorder.icon
			}
			return hover(box) to hover(text)
		} finally {
			scene.close()
			frames.close()
		}
	}

	@Test
	fun `an editor shows a hand over a task's box`() {
		assertEquals(PointerIcon.Hand to PointerIcon.Text, iconsOverTask())
	}

	@Test
	fun `a read only or disabled editor shows no hand over a box`() {
		assertEquals(PointerIcon.Text to PointerIcon.Text, iconsOverTask(readOnly = true))
		assertEquals(PointerIcon.Text to PointerIcon.Text, iconsOverTask(enabled = false))
	}

	@Test
	fun `no hand shows over a box with Shift held`() {
		assertEquals(
			PointerIcon.Text to PointerIcon.Text,
			iconsOverTask(modifiers = PointerKeyboardModifiers(isShiftPressed = true)),
		)
	}
}
