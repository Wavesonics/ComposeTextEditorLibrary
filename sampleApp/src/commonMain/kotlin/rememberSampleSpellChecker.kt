import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.darkrockstudios.texteditor.spellcheck.api.EditorSpellChecker

@Composable
expect fun rememberSampleSpellChecker(): State<EditorSpellChecker?>
