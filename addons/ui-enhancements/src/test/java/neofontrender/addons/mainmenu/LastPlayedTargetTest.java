package neofontrender.addons.mainmenu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class LastPlayedTargetTest {
    @Test
    void restoresSingleplayerAndServerTargets() {
        LastPlayedTarget world = LastPlayedTarget.persisted(
                "singleplayer", "New World", "My World", "");
        assertEquals(LastPlayedTarget.Kind.SINGLEPLAYER, world.kind());
        assertEquals("New World", world.identifier());
        assertEquals("My World", world.displayName());

        LastPlayedTarget server = LastPlayedTarget.persisted(
                "SERVER", "example.org", "Example", "example.org:25565");
        assertEquals(LastPlayedTarget.Kind.SERVER, server.kind());
        assertEquals("example.org:25565", server.address());
    }

    @Test
    void rejectsInvalidTargetsAndSanitizesLabels() {
        assertNull(LastPlayedTarget.persisted("unknown", "id", "name", ""));
        assertNull(LastPlayedTarget.singleplayer("../world", "World"));
        assertNull(LastPlayedTarget.server("", "Server"));
        assertEquals("Line One Line Two",
                LastPlayedTarget.server("localhost", "Line One\nLine Two").displayName());
    }
}
