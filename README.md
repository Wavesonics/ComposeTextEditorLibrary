_This is an exploratory project and is not ready for use._

# Compose Text Editor

I've been trying to implement a spell checking text field in Compose, and keep running up against
the limitations of `BasicTextField`. Pretty much out of options, I decided to see what it might take
to replace `BasicTextField` with something that solved all of my needs.

tl;dr it's hard, but maybe not as hard as I had thought.

This is not ready for prime time, but if anyone is interested in collaborating, I think it could
actually work. It just needs some more time and attention.

![sample_screenshot_00.png](sample_screenshot_00.png)

### What is working:

- Rich text rendering and editable
  - Semi-efficient rendering for long form text (only renders what is visible)
  - Semi-efficient data structure for text storage & editing. (but not nearly as efficient as the
    Gap Buffer BTF2 uses under the hood)
- Cursor movement, clicking, keyboard short cuts, ect
- Text selection (highlighting and edit ops)
- copy/cut/paste
- Exposed scroll state, so we can render scroll bars (BTF1 can't do this)
- Doesn't copy and return full contents on each edit, so again better for longer form text. (BTF2
  also works this way, but BTF2 doesn't support AnnotatedString for rich content)
- Support custom Rich Span drawing (this allows us to render the traditional Spell Check red
  squiggle)
- Emits edit events: so if a single character is inserted, you can collect a Flow, and know exactly
  what change was made. This makes managing Spell Check much more efficient as you can just
  respell-check the single word that was changed, rather than everything. Currently neither BTF1 or
  BTF2 offers this.

### Work left to do:

- Undo/Redo is very broken
- Apply/Remove styles at runtime is partially implemented
- Platform native text highlighting
- Write lots of tests
- I don't know, probably a lot of other things
- The code is largely prototype quality, would need a lot of spit and polish

## Want to try it?

_Keep in mind that this is not well tested, likely has many bugs._

Text Editor:
`implementation("com.darkrockstudios:composetexteditor:0.1.0")`

Text Editor with Spell Checking:
`implementation("com.darkrockstudios:composetexteditorspellcheck:0.1.0")`
