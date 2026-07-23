package neofontrender.addons.mixin;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiEnhancementsMixinConfigTest {
    @Test
    void earlyConfigDoesNotReferenceOptionalModClasses() {
        String config = config("mixins.neofontrender_ui_enhancements.json");

        assertFalse(config.contains("\"compat."));
    }

    @Test
    void optionalCompatMixinsAreLateAndNonRequired() {
        String hei = config("mixins.neofontrender_ui_enhancements_hei.json");
        String obscure = config("mixins.neofontrender_ui_enhancements_obscure_tooltips.json");

        assertTrue(hei.contains("\"required\": false"));
        assertTrue(hei.contains("\"compat.MixinHeiTooltipRenderer\""));
        assertTrue(hei.contains("\"compat.MixinHeiCollapsedGroupTooltip\""));
        assertTrue(obscure.contains("\"required\": false"));
        assertTrue(obscure.contains("\"compat.MixinObscureHeaderComponent\""));
        assertTrue(obscure.contains("\"compat.MixinObscureTooltipState\""));
    }

    private static String config(String name) {
        InputStream stream = UiEnhancementsMixinConfigTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception error) {
            throw new AssertionError("Failed to read " + name, error);
        }
    }
}
