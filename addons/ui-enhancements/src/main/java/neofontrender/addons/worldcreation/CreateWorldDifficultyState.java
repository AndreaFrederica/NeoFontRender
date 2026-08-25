package neofontrender.addons.worldcreation;

import net.minecraft.world.EnumDifficulty;

/** Carries the selected per-world difficulty into the new WorldInfo once. */
public final class CreateWorldDifficultyState {
    private static volatile EnumDifficulty pending;

    private CreateWorldDifficultyState() {}

    public static void setPending(EnumDifficulty difficulty) {
        pending = difficulty;
    }

    public static void clearPending() {
        pending = null;
    }

    public static EnumDifficulty consumePending() {
        EnumDifficulty difficulty = pending;
        pending = null;
        return difficulty;
    }
}
