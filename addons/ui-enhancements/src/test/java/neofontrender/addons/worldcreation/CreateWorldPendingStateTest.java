package neofontrender.addons.worldcreation;

import net.minecraft.world.EnumDifficulty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class CreateWorldPendingStateTest {
    @AfterEach
    void clearPendingState() {
        CreateWorldGameRulesState.clearPending();
        CreateWorldDifficultyState.clearPending();
    }

    @Test
    void gameRulesAreCopiedAndConsumedOnce() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put("keepInventory", "true");
        CreateWorldGameRulesState.setPending(input);
        input.put("keepInventory", "false");

        assertEquals("true", CreateWorldGameRulesState.consumePending().get("keepInventory"));
        assertNull(CreateWorldGameRulesState.consumePending());
    }

    @Test
    void difficultyIsConsumedOnce() {
        CreateWorldDifficultyState.setPending(EnumDifficulty.HARD);

        assertEquals(EnumDifficulty.HARD, CreateWorldDifficultyState.consumePending());
        assertNull(CreateWorldDifficultyState.consumePending());
    }
}
