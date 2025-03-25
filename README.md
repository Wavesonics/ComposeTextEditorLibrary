# Compose Text Editor

Compose has been missing a Rich Text Editor since it's inception.
I've [taken a crack](https://github.com/Wavesonics/richtext-compose-multiplatform) at this
previously, as are [others](https://github.com/MohamedRejeb/Compose-Rich-Editor).

However they have all suffered from fundamental limitations in `BasicTextField`, the foundation of
all
text entry in Compose.

This project is an attempt to re-implement text entry from scratch to finally have a
solution to the various problems.

### Why?

I've been trying to implement a [spell checking](https://github.com/Wavesonics/SymSpellKt) text
field in Compose, and keep running up against
the limitations of `BasicTextField`. Pretty much out of options, I decided to see what it might take
to replace `BasicTextField` with something that solved all of my needs.

**tl;dr** it's hard, _but maybe not as hard as I had thought_.

~~This is not ready for prime time, but if anyone is interested in collaborating, I think it could
actually work. It just needs some more time and attention.~~

Maybe it's ready? [Give it a try here](https://wavesonics.github.io/ComposeTextEditorLibrary/).

![sample_screenshot_00.png](sample_screenshot_00.png)

### What is working:

- Rich text rendering and editable
  - Semi-efficient rendering for long form text (_only renders what is visible_)
  - Semi-efficient data structure for text storage & editing. (_but not nearly as efficient as the
    Gap Buffer BTF2 uses under the hood_)
- Cursor movement, clicking, keyboard short cuts, ect
- Text selection (_highlighting and edit ops_)
- copy/cut/paste
- Exposed scroll state, so we can render scroll bars (_BTF1 can't do this_)
- Doesn't copy and return full contents on each edit, so again better for longer form text. (_BTF2
  also works this way, but BTF2 doesn't support AnnotatedString for rich content_)
- Support custom Rich Span drawing (_this allows us to render the traditional Spell Check red
  squiggle_)
- Emits edit events: so if a single character is inserted, you can collect a Flow, and know exactly
  what change was made. This makes managing Spell Check much more efficient as you can just
  respell-check the single word that was changed, rather than everything. Currently neither BTF1 or
  BTF2 offers this.

#### Platforms

So far I have support for JVM/Desktop, Android, Web. The others should probably also work,
but I haven't had time to add the targets and test them.

### Work left to do:

- Copy/Paste of rich text always strips the formatting (_this is a Compose MP bug_)
- Right-to-Left text is probably broken

## Want to try it?

Text Editor:

`implementation("com.darkrockstudios:composetexteditor:0.7.0")`

Text Editor with Spell Checking:

`implementation("com.darkrockstudios:composetexteditorspellcheck:0.7.0")`
