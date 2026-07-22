package neofontrender.addons.hud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudBarsConfigSnapshotTest {
    private final boolean initialEnabled = HudBarsConfig.enabled;
    private final int initialWidth = HudBarsConfig.width;
    private final String initialTheme = HudBarsConfig.theme;

    @AfterEach
    void restoreInitialState() {
        HudBarsConfig.enabled = initialEnabled;
        HudBarsConfig.width = initialWidth;
        HudBarsConfig.theme = initialTheme;
    }

    @Test
    void rollbackRestoresRuntimeBeforePersistingSnapshot() {
        HudBarsConfig.enabled = true;
        HudBarsConfig.width = 81;
        HudBarsConfig.theme = HudBarTheme.MODERN.id;
        AtomicBoolean persisted = new AtomicBoolean();
        HudBarsConfigSnapshot snapshot = HudBarsConfigSnapshot.capture(() -> {
            assertTrue(HudBarsConfig.enabled);
            assertEquals(81, HudBarsConfig.width);
            assertEquals(HudBarTheme.MODERN.id, HudBarsConfig.theme);
            persisted.set(true);
        });

        HudBarsConfig.enabled = false;
        HudBarsConfig.width = 144;
        HudBarsConfig.theme = HudBarTheme.MINIMAL.id;
        snapshot.restoreAndPersist();

        assertTrue(persisted.get());
        assertTrue(HudBarsConfig.enabled);
        assertEquals(81, HudBarsConfig.width);
        assertEquals(HudBarTheme.MODERN.id, HudBarsConfig.theme);
    }
}
