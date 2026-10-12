package neofontrender.addons.compat.thaumcraft;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThaumcraftCommandCompletionTest {
    private static final java.util.List<String> PLAYERS = Arrays.asList("Alice", "Bob");
    private static final java.util.List<String> RESEARCH =
            Arrays.asList("ALCHEMY", "BASICS", "INFUSION");

    @Test
    void completesRootActions() {
        assertEquals(Arrays.asList("help", "reload", "research", "warp"),
                complete(""));
        assertEquals(Collections.singletonList("research"), complete("res"));
    }

    @Test
    void completesResearchPlayersActionsAndKeys() {
        assertEquals(Arrays.asList("list", "Alice", "Bob"),
                complete("research", ""));
        assertEquals(Arrays.asList("list", "all", "reset", "revoke",
                        "ALCHEMY", "BASICS", "INFUSION"),
                complete("research", "Alice", ""));
        assertEquals(Collections.singletonList("INFUSION"),
                complete("research", "Alice", "revoke", "inf"));
        assertEquals(Collections.emptyList(), complete("research", "list", ""));
    }

    @Test
    void completesWarpGrammar() {
        assertEquals(Collections.singletonList("Bob"), complete("warp", "B"));
        assertEquals(Arrays.asList("add", "set"), complete("warp", "Bob", ""));
        assertEquals(Collections.emptyList(), complete("warp", "Bob", "set", ""));
        assertEquals(Arrays.asList("PERM", "TEMP"),
                complete("warp", "Bob", "set", "10", ""));
    }

    private static java.util.List<String> complete(String... arguments) {
        return ThaumcraftCommandCompletionProvider.complete(arguments, PLAYERS, RESEARCH);
    }
}
