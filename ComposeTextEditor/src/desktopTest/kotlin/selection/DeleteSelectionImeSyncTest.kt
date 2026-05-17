package selection

import androidx.compose.ui.text.AnnotatedString
import com.darkrockstudios.texteditor.CharLineOffset
import com.darkrockstudios.texteditor.state.TextEditorState
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.test.TestScope
import kotlin.test.*

/**
 * Regression for the wild crash reported in hammer-editor #492:
 *
 *   java.lang.IndexOutOfBoundsException: Index N out of bounds for length N
 *     at TextEditorState.getCharacterIndex(...)
 *     at ImeCursorSync$startSync$1$2.emit(...)
 *     ... (flow combine) ...
 *     at TextEditorState.delete(...)
 *
 * `ImeCursorSync` combines [TextEditorState.cursor.positionFlow] with
 * [TextEditorSelectionManager.selectionRangeFlow] on Dispatchers.Main.immediate,
 * and inside the collector it calls [TextEditorState.getCharacterIndex] on the
 * selection endpoints.
 *
 * `TextEditorSelectionManager.deleteSelection` mutates `textLines` and emits
 * the new cursor position *before* it clears the selection flow. With
 * Main.immediate the collector fires synchronously between those two steps,
 * sees `(newCursor, staleSelection)`, and `getCharacterIndex` walks off the
 * end of the truncated `textLines`.
 */
class DeleteSelectionImeSyncTest {

	private lateinit var state: TextEditorState
	private lateinit var imeScope: CoroutineScope
	private val syncErrors = mutableListOf<Throwable>()
	private var collectorJob: Job? = null

	@BeforeTest
	fun setup() {
		state = TextEditorState(
			scope = TestScope(),
			measurer = mockk(relaxed = true),
			initialText = AnnotatedString(""),
		)
		// Unconfined matches `Dispatchers.Main.immediate`'s re-entrancy: the
		// collector resumes synchronously inside the tryEmit call, which is the
		// only configuration in which the production crash reproduces.
		imeScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
		syncErrors.clear()
	}

	@AfterTest
	fun tearDown() {
		collectorJob?.cancel()
		imeScope.cancel()
	}

	private fun startImeStyleCollector() {
		collectorJob = imeScope.launch {
			combine(
				state.cursor.positionFlow,
				state.selector.selectionRangeFlow,
			) { cursor, selection -> cursor to selection }
				.collect { (cursor, selection) ->
					try {
						if (selection != null) {
							state.getCharacterIndex(selection.start)
							state.getCharacterIndex(selection.end)
						} else {
							state.getCharacterIndex(cursor)
						}
					} catch (t: Throwable) {
						syncErrors.add(t)
					}
				}
		}
	}

	@Test
	fun `deleting a multi-line selection does not crash the IME flow combine`() {
		val text = (0..30).joinToString("\n") { "Line $it has some content" }
		state.setText(text)

		// Subscribe FIRST so the collector sees subsequent emissions —
		// these SharedFlows have replay=0 and would drop anything emitted
		// before the collect call.
		startImeStyleCollector()

		// Prime both upstreams so `combine` has a value from each. Without
		// this, the first cursor emit inside the delete would not produce
		// a combine emission (combine waits for all upstreams).
		state.cursor.updatePosition(CharLineOffset(25, 5))
		state.selector.updateSelection(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(25, 5),
		)

		// Sanity: the priming emissions should have run cleanly.
		assertTrue(syncErrors.isEmpty(), "Priming crashed: ${syncErrors.firstOrNull()}")

		state.selector.deleteSelection()

		assertTrue(
			syncErrors.isEmpty(),
			"IME-style collector crashed during deleteSelection: ${syncErrors.firstOrNull()}",
		)
		assertNull(state.selector.selection, "Selection should be cleared after deleteSelection")
	}

	@Test
	fun `direct state delete with an active selection does not crash the IME flow combine`() {
		// Locks down the invariant at the applyOperation layer: any text
		// mutation should invalidate the selection before its cursor emit
		// fires, not just deleteSelection. Anyone calling state.delete(range)
		// directly (e.g. a future feature, undo, programmatic edit) gets the
		// same protection.
		val text = (0..30).joinToString("\n") { "Line $it has some content" }
		state.setText(text)

		startImeStyleCollector()

		state.cursor.updatePosition(CharLineOffset(25, 5))
		state.selector.updateSelection(
			start = CharLineOffset(0, 0),
			end = CharLineOffset(25, 5),
		)

		assertTrue(syncErrors.isEmpty(), "Priming crashed: ${syncErrors.firstOrNull()}")

		// Direct delete — bypasses deleteSelection's clearSelection call.
		state.delete(
			com.darkrockstudios.texteditor.TextEditorRange(
				start = CharLineOffset(0, 0),
				end = CharLineOffset(25, 5),
			),
		)

		assertTrue(
			syncErrors.isEmpty(),
			"IME-style collector crashed during state.delete: ${syncErrors.firstOrNull()}",
		)
	}

	@Test
	fun `deleting a single-line selection does not crash the IME flow combine`() {
		// Crash C from the report — small delete, same crash shape (Index N for length N).
		state.setText("Hello world, this is a single line of text")

		startImeStyleCollector()

		state.cursor.updatePosition(CharLineOffset(0, 11))
		state.selector.updateSelection(
			start = CharLineOffset(0, 6),
			end = CharLineOffset(0, 11),
		)

		assertTrue(syncErrors.isEmpty(), "Priming crashed: ${syncErrors.firstOrNull()}")

		state.selector.deleteSelection()

		assertTrue(
			syncErrors.isEmpty(),
			"IME-style collector crashed during deleteSelection: ${syncErrors.firstOrNull()}",
		)
		assertNull(state.selector.selection)
	}
}
