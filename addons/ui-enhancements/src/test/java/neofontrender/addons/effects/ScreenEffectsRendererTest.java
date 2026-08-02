package neofontrender.addons.effects;

import net.minecraft.client.gui.GuiChat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ScreenEffectsRendererTest {
    @Test
    void chatNeverUsesFullScreenGradientOverlay() {
        boolean originalEnabled = ScreenEffectsConfig.enabled;
        boolean originalGradient = ScreenEffectsConfig.gradient;
        try {
            ScreenEffectsConfig.enabled = true;
            ScreenEffectsConfig.gradient = true;

            assertFalse(ScreenEffectsRenderer.usesOverlay(new GuiChat()));
        } finally {
            ScreenEffectsConfig.enabled = originalEnabled;
            ScreenEffectsConfig.gradient = originalGradient;
        }
    }
}
