import androidx.compose.runtime.*
import com.darkrockstudios.libs.platformspellchecker.PlatformSpellCheckerFactory
import com.darkrockstudios.libs.platformspellchecker.SpLocale
import com.darkrockstudios.texteditor.spellcheck.adapters.PlatformEditorSpellChecker
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun rememberSampleSpellChecker(): State<EditorSpellChecker?> {
	val checker: MutableState<EditorSpellChecker?> = remember { mutableStateOf(null) }

	LaunchedEffect(Unit) {
		val spellChecker = PlatformSpellCheckerFactory().createSpellChecker(SpLocale.EN_US)
		checker.value = PlatformEditorSpellChecker(spellChecker)
	}

	return checker
}
