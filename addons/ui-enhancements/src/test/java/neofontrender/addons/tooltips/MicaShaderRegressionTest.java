package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicaShaderRegressionTest {
    @Test
    void capturesAfterHudBeforeTheLowPriorityScreenGradient() {
        String classMetadata = classMetadata("TooltipModule.class");

        assertTrue(classMetadata.contains("captureMicaSceneAfterHud"));
        assertTrue(classMetadata.contains("RenderGameOverlayEvent$Post"));
        assertTrue(classMetadata.contains("HIGHEST"));
    }

    @Test
    void backdropFilterDoesNotBakeInTheOldNearBlackTint() {
        String shader = shader("mica_backdrop.fsh");

        assertFalse(shader.contains("0.92"));
        assertFalse(shader.contains("color *= 0.80"));
    }

    @Test
    void materialUsesFillAlphaAsTintAndKeepsFallbackTranslucent() {
        String shader = shader("modern_tooltip.fsh");

        assertTrue(shader.contains("fill.a * 0.55"));
        assertTrue(shader.contains("fill.a = min(fill.a, 0.92)"));
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
