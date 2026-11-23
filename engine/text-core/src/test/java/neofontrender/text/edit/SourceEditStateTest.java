package neofontrender.text.edit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceEditStateTest {
    @Test
    void caretInsideSpanRevealsSource() {
        SourceInlineSpan span = new SourceInlineSpan(2, 8,
                new neofontrender.text.InlineContent("test", "key", "test", true,
                        null, null, null));
        assertEquals(SourcePreviewMode.RAW,
                SourcePreviewResolver.CARET_REVEAL.mode(span, SourceEditState.of("0123456789", 5)));
        assertEquals(SourcePreviewMode.PREVIEW,
                SourcePreviewResolver.CARET_REVEAL.mode(span, SourceEditState.of("0123456789", 9)));
    }

    @Test
    void stateClampsCursorAndSelection() {
        SourceEditState state = new SourceEditState("abc", 99, -1, 99);
        assertEquals(3, state.cursor());
        assertEquals(0, state.selectionStart());
        assertEquals(3, state.selectionEnd());
        assertTrue(state.hasSelection());
    }
}
