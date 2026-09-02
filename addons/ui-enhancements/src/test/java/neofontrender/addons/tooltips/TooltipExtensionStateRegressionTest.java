package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void preservesStateOnlyForModularUiRichTooltipEvents() {
        assertTrue(ModernTooltipHandler.preservesCallerState(
                "com.cleanroommc.modularui.screen.RichTooltipEvent$Pre"));
        assertFalse(ModernTooltipHandler.preservesCallerState(
                "net.minecraftforge.client.event.RenderTooltipEvent$Pre"));
        assertFalse(ModernTooltipHandler.preservesCallerState(null));
    }

    @Test
    void synchronizesDriverAndGlStateManagerAfterModularUiTooltip() {
        String metadata = classMetadata("ModernTooltipRenderer$CallerGlState.class");

        assertTrue(metadata.contains("glPushAttrib"));
        assertTrue(metadata.contains("glPopAttrib"));
        assertTrue(metadata.contains("glBlendFuncSeparate"));
        assertTrue(metadata.contains("enableBlend"));
        assertTrue(metadata.contains("disableBlend"));
        assertTrue(metadata.contains("enableDepth"));
        assertTrue(metadata.contains("disableDepth"));
        assertTrue(metadata.contains("setActiveTexture"));
        assertTrue(metadata.contains("restoreTextureUnits"));
        assertTrue(metadata.contains("readTextureUnits"));
        assertTrue(metadata.contains("enableTexture2D"));
        assertTrue(metadata.contains("disableTexture2D"));
        assertTrue(metadata.contains("bindTexture"));
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
