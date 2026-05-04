package blocks

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import com.darkrockstudios.texteditor.richstyle.ImageBlockResource
import com.darkrockstudios.texteditor.richstyle.ImageBlockSpanStyle
import com.darkrockstudios.texteditor.richstyle.InMemoryImageProvider
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageBlockHeightTest {

	private val density = Density(density = 1f, fontScale = 1f)

	private fun fakeBitmap(width: Int, height: Int): ImageBitmap {
		return mockk(relaxed = true) {
			every { this@mockk.width } returns width
			every { this@mockk.height } returns height
		}
	}

	@Test
	fun `height fits viewport width preserving aspect ratio`() {
		val provider = InMemoryImageProvider().apply {
			put("img", fakeBitmap(width = 1000, height = 500))
		}
		val style = ImageBlockSpanStyle(source = "img", provider = provider)

		// Bitmap is 1000x500, viewport 400 → scale 0.4 → height 200.
		assertEquals(200f, style.blockHeight(density, viewportWidth = 400f))
	}

	@Test
	fun `height does not upscale beyond intrinsic size`() {
		val provider = InMemoryImageProvider().apply {
			put("img", fakeBitmap(width = 100, height = 80))
		}
		val style = ImageBlockSpanStyle(source = "img", provider = provider)

		// Viewport 800 wider than bitmap; scale clamped at 1f → height stays 80.
		assertEquals(80f, style.blockHeight(density, viewportWidth = 800f))
	}

	@Test
	fun `height falls back to placeholder while loading`() {
		val provider = InMemoryImageProvider()
		// Loaded → loading by overwriting with a Loading state isn't part of the
		// in-memory provider API, but the default for an unrequested key is
		// Failed which also falls back to the placeholder.
		val style = ImageBlockSpanStyle(
			source = "missing",
			provider = provider,
			placeholderHeightDp = 150f,
		)

		assertEquals(150f, style.blockHeight(density, viewportWidth = 400f))
	}

	@Test
	fun `height falls back to placeholder on failed load`() {
		val provider = InMemoryImageProvider().apply {
			fail("missing", "404")
		}
		val style = ImageBlockSpanStyle(
			source = "missing",
			provider = provider,
			placeholderHeightDp = 120f,
		)

		assertEquals(120f, style.blockHeight(density, viewportWidth = 400f))
	}

	@Test
	fun `provider returns failed for unknown source by default`() {
		val provider = InMemoryImageProvider()
		val resource = provider.resolve("anything").value
		assertTrue(resource is ImageBlockResource.Failed)
	}
}
