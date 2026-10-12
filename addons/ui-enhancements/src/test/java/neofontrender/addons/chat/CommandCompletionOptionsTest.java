package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandCompletionOptionsTest {
    @Test void supportsEveryIndependentEngineAndDisplayCombination() {
        for (String engine : CommandCompletionOptions.engines()) {
            assertEquals(engine, CommandCompletionOptions.engine(engine));
        }
        for (String display : CommandCompletionOptions.displays()) {
            assertEquals(display, CommandCompletionOptions.display(display));
        }
    }

    @Test void missingOrInvalidProvidersFallBackWithoutLosingHiddenMode() {
        assertEquals("uie", CommandCompletionOptions.engine("unknown-provider"));
        assertEquals("uie", CommandCompletionOptions.display("unknown-provider"));
        assertEquals("hidden", CommandCompletionOptions.display("hidden"));
        assertEquals("uie", CommandCompletionOptions.engine(null));
        assertEquals("uie", CommandCompletionOptions.display("invalid"));
    }
}
