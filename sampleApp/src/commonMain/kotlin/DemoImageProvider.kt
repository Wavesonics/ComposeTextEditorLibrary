import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.darkrockstudios.texteditor.richstyle.InMemoryImageProvider
import composetexteditorlibrary.sampleapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Maps markdown image `source` keys to bundled Compose resource paths. */
private val DEFAULT_DEMO_IMAGE_SOURCES = mapOf("sample.jpg" to "files/sample.jpg")

/**
 * Wires the demo's markdown image syntax (`![alt](sample.jpg)`) to bundled
 * Compose resources. Each declared `source` is decoded once and pushed into an
 * [InMemoryImageProvider]; the editor recomposes when the loaded bitmap arrives.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberDemoImageProvider(
	sources: Map<String, String> = DEFAULT_DEMO_IMAGE_SOURCES,
): InMemoryImageProvider {
	val provider = remember { InMemoryImageProvider() }
	LaunchedEffect(sources) {
		sources.forEach { (key, resourcePath) ->
			try {
				val bitmap = withContext(Dispatchers.Default) {
					Res.readBytes(resourcePath).decodeToImageBitmap()
				}
				provider.put(key, bitmap)
			} catch (t: Throwable) {
				provider.fail(key, t.message ?: "load failed")
			}
		}
	}
	return provider
}
