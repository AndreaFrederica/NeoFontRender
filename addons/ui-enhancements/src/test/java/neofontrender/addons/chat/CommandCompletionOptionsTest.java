package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandCompletionOptionsTest {
    @Test void supportsEveryIndependentEngineAndDisplayCombination() {
        for (String engine : new String[] {"uie", "pregenerator"}) {
            for (String display : new String[] {"uie", "pregenerator", "hidden"}) {
                assertEquals(engine, CommandCompletionOptions.engine(engine, true));
                assertEquals(display, CommandCompletionOptions.display(display, true));
            }
        }
    }

    @Test void missingOrInvalidProvidersFallBackWithoutLosingHiddenMode() {
        assertEquals("uie", CommandCompletionOptions.engine("pregenerator", false));
        assertEquals("uie", CommandCompletionOptions.display("pregenerator", false));
        assertEquals("hidden", CommandCompletionOptions.display("hidden", false));
        assertEquals("uie", CommandCompletionOptions.engine(null, true));
        assertEquals("uie", CommandCompletionOptions.display("invalid", true));
    }

    @Test void switchingProvidersRejectsAnOldResponseEvenForIdenticalInput() {
        ChatCommandCompletionController.RequestTracker tracker = new ChatCommandCompletionController.RequestTracker();
        tracker.beginRequest("/pregen", new String[] {"old"});
        tracker.deactivate();
        tracker.resetPrefix();
        assertTrue(tracker.shouldRequest("/pregen"));
        tracker.beginRequest("/pregen", new String[] {"new"});
        assertNull(tracker.acceptResponse("/pregen"));
        assertArrayEquals(new String[] {"new"}, tracker.acceptResponse("/pregen").clientValues);
    }

    @Test void ownershipOverrideDoesNotAlterOtherPregenOptions() {
        assertTrue(PregenChatCompat.effectiveOption(false, true, true));
        assertFalse(PregenChatCompat.effectiveOption(false, true, false));
        assertFalse(PregenChatCompat.effectiveOption(false, false, true));
        assertTrue(PregenChatCompat.effectiveOption(true, false, true));
        assertTrue(PregenChatCompat.effectiveOption(true, true, false));
    }
}
