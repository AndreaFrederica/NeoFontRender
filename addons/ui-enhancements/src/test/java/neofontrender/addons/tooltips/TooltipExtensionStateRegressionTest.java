package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipExtensionStateRegressionTest {
    @Test
    void keepsForgeTooltipRenderStateForPostEventSubscribers() {
        String metadata = classMetadata("ModernTooltipRenderer.class");

        assertTrue(metadata.contains("beginTooltipExtensions"));
        assertTrue(metadata.contains("endTooltipExtensions"));
        assertTrue(metadata.contains("disableRescaleNormal"));
        assertTrue(metadata.contains("disableStandardItemLighting"));
        assertTrue(metadata.contains("disableLighting"));
        assertTrue(metadata.contains("disableDepth"));
        assertTrue(metadata.contains("enableGUIStandardItemLighting"));
        assertTrue(metadata.contains("enableRescaleNormal"));
        assertTrue(metadata.contains("RenderTooltipEvent$PostBackground"));
        assertTrue(metadata.contains("RenderTooltipEvent$PostText"));
    }

    private static String classMetadata(String name) {
        String path = "neofontrender/addons/tooltips/" + name;
        InputStream stream = TooltipExtensionStateRegressionTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(stream, path);
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        } catch (Exception error) {
            throw new AssertionError("Failed to read " + path, error);
        }
    }
}
