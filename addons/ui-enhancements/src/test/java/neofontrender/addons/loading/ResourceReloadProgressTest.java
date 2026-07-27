package neofontrender.addons.loading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResourceReloadProgressTest {
    @Test
    void nestedForgeBarsMapToMonotonicStages() {
        ResourceReloadProgress progress = new ResourceReloadProgress();
        progress.reset();
        float previous = progress.amount();

        float[] values = {
                progress.step("Loading Resources", 0, 4),
                progress.step("Loading Resources", 3, 4),
                progress.step("Reloading", 0, 20),
                progress.step("Reloading", 10, 20),
                progress.completeBar("Reloading"),
                progress.completeBar("Loading Resources"),
                progress.languageMetadata(),
                progress.rendererRefresh(),
                progress.complete()
        };

        for (float value : values) {
            assertTrue(value >= previous);
            previous = value;
        }
        assertEquals(1.0F, progress.amount(), 0.0001F);
    }

    @Test
    void unknownAndEmptyBarsDoNotInventProgress() {
        ResourceReloadProgress progress = new ResourceReloadProgress();
        progress.reset();
        assertEquals(0.02F, progress.step("Unrelated", 5, 10), 0.0001F);
        assertEquals(0.02F, progress.step("Reloading", 0, 0), 0.0001F);
    }
}
