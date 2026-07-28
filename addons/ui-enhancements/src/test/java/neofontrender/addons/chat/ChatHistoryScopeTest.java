package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHistoryScopeTest {
    @Test
    void isolatesSingleplayerSaveFolders() {
        assertNotEquals(ChatHistoryScope.singleplayer("World One"),
                ChatHistoryScope.singleplayer("World Two"));
        assertTrue(ChatHistoryScope.valid(ChatHistoryScope.singleplayer("World One")));
    }

    @Test
    void isolatesServersAndNormalizesAddressCase() {
        assertNotEquals(ChatHistoryScope.server("one.example:25565"),
                ChatHistoryScope.server("two.example:25565"));
        assertEquals(ChatHistoryScope.server("PLAY.EXAMPLE:25565"),
                ChatHistoryScope.server("play.example:25565"));
    }

    @Test
    void rejectsMissingTargets() {
        assertNull(ChatHistoryScope.singleplayer("  "));
        assertNull(ChatHistoryScope.server(null));
    }
}
