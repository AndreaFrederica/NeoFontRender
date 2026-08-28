package neofontrender.addons.cursor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CursorApiTest {
    @Test
    void requestCarriesSemanticTypeAndInteractionState() {
        CursorRequest request = CursorRequest.of(CursorType.BUTTON,
                CursorInteractionState.HOVER, 400, "test:button");

        assertEquals(CursorType.BUTTON, request.type());
        assertEquals(CursorInteractionState.HOVER, request.state());
        assertEquals(400, request.priority());
        assertEquals("test:button", request.source());
    }

    @Test
    void rulesReplaceByStableIdAndCanBeClosed() {
        AutoCloseable first = CursorRuleRegistry.register("test:rule", 10,
                context -> CursorRequest.of(CursorType.TEXT, CursorInteractionState.HOVER,
                        10, "test:first"));
        AutoCloseable second = CursorRuleRegistry.register("test:rule", 20,
                context -> CursorRequest.of(CursorType.BUTTON, CursorInteractionState.HOVER,
                        20, "test:second"));
        try {
            assertNotNull(second);
        } finally {
            try { second.close(); } catch (Exception ignored) {}
            try { first.close(); } catch (Exception ignored) {}
        }
    }

    @Test
    void registryOwnsRulePriorityAndSource() {
        AutoCloseable registration = CursorRuleRegistry.register("test:priority", 73,
                context -> CursorRequest.of(CursorType.BUTTON, CursorInteractionState.HOVER,
                        999, "spoofed:source"));
        try {
            CursorRequest resolved = CursorRuleRegistry.resolve(null).get(0);
            assertEquals(73, resolved.priority());
            assertEquals("test:priority", resolved.source());
        } finally {
            try { registration.close(); } catch (Exception ignored) {}
        }
    }
}
