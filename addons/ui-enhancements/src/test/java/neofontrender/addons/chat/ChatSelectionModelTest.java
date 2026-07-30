package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSelectionModelTest {
    private final List<String> bottomUp = Arrays.asList("bottom", "middle", "top");

    @Test
    void selectsAcrossBottomUpStorageInReadingOrder() {
        ChatSelectionModel<String> selection = new ChatSelectionModel<>();
        selection.begin(bottomUp.get(2), 1);
        selection.update(bottomUp.get(0), 3);
        assertEquals("op\nmiddle\nbot", selection.selectedText(bottomUp, value -> value));
    }

    @Test
    void reverseDragProducesTheSameText() {
        ChatSelectionModel<String> selection = new ChatSelectionModel<>();
        selection.begin(bottomUp.get(0), 3);
        selection.update(bottomUp.get(2), 1);
        assertEquals("op\nmiddle\nbot", selection.selectedText(bottomUp, value -> value));
    }

    @Test
    void emptyDragIsNotASelection() {
        ChatSelectionModel<String> selection = new ChatSelectionModel<>();
        selection.begin(bottomUp.get(1), 2);
        assertFalse(selection.hasSelection());
        selection.update(bottomUp.get(1), 4);
        assertTrue(selection.hasSelection());
        assertEquals("dd", selection.selectedText(bottomUp, value -> value));
    }
}
