package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicaShaderRegressionTest {
    @Test
    void capturesBeforeTheGuiDraws() {
        String moduleMetadata = classMetadata("TooltipModule.class");
        String handlerMetadata = classMetadata("ModernTooltipHandler.class");

        assertTrue(moduleMetadata.contains("captureMicaScene"));
        assertTrue(moduleMetadata.contains("GuiScreenEvent$DrawScreenEvent$Pre"));
        assertTrue(moduleMetadata.contains("HIGHEST"));
        assertFalse(handlerMetadata.contains("captureScene"));
    }

    @Test
    void backdropFilterBakesInTheDarkMaterialTint() {
        String shader = shader("mica_backdrop.fsh");

        assertTrue(shader.contains("srgbToLinear(vec3(0.032, 0.036, 0.048)), 0.92"));
        assertTrue(shader.contains("color *= 0.80"));
    }

    @Test
    void materialUsesTheBackdropDirectlyAndKeepsAnOpaqueFallback() {
        String shader = shader("modern_tooltip.fsh");

        assertTrue(shader.contains("fill.rgb = texture2D(uBackdrop, backdropUv).rgb"));
        assertTrue(shader.contains("fill.rgb = vec3(0.055, 0.060, 0.075)"));
        assertTrue(shader.contains("fill.a = 1.0"));
        assertFalse(shader.contains("fill.a * 0.55"));
        assertFalse(shader.contains("fill.a = min(fill.a, 0.92)"));
    }

    private static String shader(String name) {
        return resource("assets/neofontrender_ui_enhancements/shaders/" + name,
                StandardCharsets.UTF_8);
    }

    private static String classMetadata(String name) {
        return resource("neofontrender/addons/tooltips/" + name,
                StandardCharsets.ISO_8859_1);
    }

    private static String resource(String path, java.nio.charset.Charset charset) {
        InputStream stream = MicaShaderRegressionTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(stream, path);
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), charset);
        } catch (Exception error) {
            throw new AssertionError("Failed to read " + path, error);
        }
    }
}
