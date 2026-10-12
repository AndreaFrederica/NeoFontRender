package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipExtensionStateRegressionTest {
    @Test
    void rendererDisablesStandardItemLightingForItsOwnPasses() {
        String metadata = classMetadata("ModernTooltipRenderer.class");

        assertTrue(metadata.contains("disableStandardItemLighting"));
        assertTrue(metadata.contains("CallerGlState"));
    }

    @Test
    void callerGlStateSnapshotsAndRestoresDriverState() {
        String metadata = classMetadata("ModernTooltipRenderer$CallerGlState.class");

        // 1.7.10 has no GlStateManager cache to repair, so the attribute stack plus the texture
        // bindings of the units item rendering touches are what keeps the caller's state intact.
        assertTrue(metadata.contains("glPushAttrib"));
        assertTrue(metadata.contains("glPopAttrib"));
        assertTrue(metadata.contains("glActiveTexture"));
        assertTrue(metadata.contains("glBindTexture"));
        assertTrue(metadata.contains("glGetInteger"));
    }

    @Test
    void isolatesModelNodesWhenAComponentRendererCallsThemDirectly() {
        String metadata = classMetadata("TooltipVisualPlan$ExternalBlock.class");

        assertTrue(metadata.contains("ModernTooltipRenderer$CallerGlState"));
        assertTrue(metadata.contains("enableGUIStandardItemLighting"));
        assertTrue(metadata.contains("disableStandardItemLighting"));
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
