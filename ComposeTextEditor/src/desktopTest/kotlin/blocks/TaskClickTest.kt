package blocks

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.TaskChecking
import com.darkrockstudios.texteditor.pointerIconAt
import com.darkrockstudios.texteditor.richstyle.TaskSpanStyle
import com.darkrockstudios.texteditor.state.taskCheckedAt
import com.darkrockstudios.texteditor.utils.lineTextLeft
import org.junit.Test
import utils.EditorUiTestScope
import utils.editorUiTest
import utils.positionOfCharacter
import utils.setBlockLines
import kotlin.test.assertEquals

/** A click or a tap on a task's box checks it; on its text it places the caret as anywhere. */
@OptIn(ExperimentalTestApi::class)
class TaskClickTest {

	/** The middle of the box of the task on [line], in text canvas coordinates. */
	private fun EditorUiTestScope.boxInCanvas(line: Int): Offset = test.runOnIdle {
		val row = state.lineOffsets.first { it.line == line }
		val (topLeft, size) = TaskSpanStyle.checkboxBounds(row.textLayoutResult, state.density!!)
		row.offset + topLeft + Offset(size.width / 2f, size.height / 2f) - Offset(0f, state.scrollState.value.toFloat())
	}

	/** The middle of the box of the task on [line], in editor node coordinates. */
	private fun EditorUiTestScope.boxOf(line: Int): Offset = canvasToNode(boxInCanvas(line))

	@Test
	fun `a click on a task's box checks and unchecks it, a tap too`() = editorUiTest(initialText = AnnotatedString("")) {
		test.runOnIdle { state.setBlockLines("- [ ] first\n- [x] second") }

		mouse { click(boxOf(0)) }
		assertEquals(true, test.runOnIdle { state.taskCheckedAt(0) })

		tapAt(boxOf(1))
		assertEquals(false, test.runOnIdle { state.taskCheckedAt(1) })

		mouse { click(positionOfCharacter(4)) }
		assertEquals(true, test.runOnIdle { state.taskCheckedAt(0) })
	}

	@Test
	fun `the caret's task answers to an accessibility action`() = editorUiTest(initialText = AnnotatedString("")) {
		test.runOnIdle {
			state.setBlockLines("- [ ] first")
			state.cursor.updatePosition(com.darkrockstudios.texteditor.CharLineOffset(0, 2))
		}

		test.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText)).performCustomAccessibilityActionWithLabel("Check task")
		assertEquals(true, test.runOnIdle { state.taskCheckedAt(0) })
		test.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText)).performCustomAccessibilityActionWithLabel("Uncheck task")
		assertEquals(false, test.runOnIdle { state.taskCheckedAt(0) })
	}

	@Test
	fun `a read only editor's boxes do not change`() = editorUiTest(initialText = AnnotatedString(""), readOnly = true) {
		test.runOnIdle { state.setBlockLines("- [ ] first") }

		mouse { click(boxOf(0)) }
		assertEquals(false, test.runOnIdle { state.taskCheckedAt(0) })
		tapAt(boxOf(0))
		assertEquals(false, test.runOnIdle { state.taskCheckedAt(0) })
	}

	@Test
	fun `a hand shows over a task's box where a click would check it`() = editorUiTest(initialText = AnnotatedString("")) {
		test.runOnIdle { state.setBlockLines("- [ ] first") }
		fun iconAt(offset: Offset, checks: Boolean, modifiers: PointerKeyboardModifiers = PointerKeyboardModifiers()) =
			pointerIconAt(state, offset, modifiers, null, PointerIcon.Text, TaskChecking(state, { checks }, { null }))

		val box = boxInCanvas(0)
		assertEquals(PointerIcon.Hand, iconAt(box, checks = true))
		assertEquals(PointerIcon.Text, iconAt(test.runOnIdle { state.positionOfCharacter(4) }, checks = true))
		assertEquals(PointerIcon.Text, iconAt(box, checks = false))
		assertEquals(PointerIcon.Text, iconAt(box, checks = true, PointerKeyboardModifiers(isShiftPressed = true)))
	}

	@Test
	fun `the box's hit area stops at the text`() = editorUiTest(initialText = AnnotatedString("")) {
		test.runOnIdle { state.setBlockLines("- [ ] first") }
		val textLeft = test.runOnIdle {
			val row = state.lineOffsets.first { it.line == 0 }
			canvasToNode(row.offset + Offset(row.textLayoutResult.lineTextLeft(0, state.density), 0f))
		}
		val onFirstGlyph = Offset(textLeft.x + 0.25f, boxOf(0).y)

		mouse { click(onFirstGlyph) }

		assertEquals(false, test.runOnIdle { state.taskCheckedAt(0) })
	}
}
