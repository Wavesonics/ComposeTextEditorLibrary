import androidx.compose.runtime.*
import com.darkrockstudios.symspell.fdic.loadFdicFile
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.impl.SymSpell
import com.darkrockstudios.texteditor.spellcheck.adapters.SymSpellEditorSpellChecker
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker
import composetexteditorlibrary.sampleapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun rememberSampleSpellChecker(): State<EditorSpellChecker?> {
	val checker: MutableState<EditorSpellChecker?> = remember { mutableStateOf(null) }

	LaunchedEffect(Unit) {
		val wrapped = withContext(Dispatchers.Default) {
			val sym = SymSpell(spellCheckSettings = SpellCheckSettings(topK = 5))
			sym.dictionary.loadFdicFile(Res.readBytes("files/en-80k.fdic"))
			SymSpellEditorSpellChecker(sym) as EditorSpellChecker
		}
		checker.value = wrapped
	}

	return checker
}
