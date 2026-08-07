package neofontrender.addons.mixin;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiEnhancementsMixinConfigTest {
    @Test
    void hoverMixinsAreInTheRequiredEarlyConfig() {
        String config = config("mixins.neofontrender_ui_enhancements.json");

        assertTrue(config.contains("\"MixinGuiButtonHover\""));
        assertTrue(config.contains("\"MixinGuiButtonExtHover\""));
        assertTrue(config.contains("\"MixinForgeGuiUtilsButtonAlpha\""));
        assertTrue(config.contains("\"MixinGuiContainerSlotHover\""));
        assertTrue(config.contains("\"MixinModularItemSlotHover\""));
        assertTrue(config.contains("\"compat.MixinNeiIngredientGridHover\""));
        assertTrue(config.contains("\"compat.MixinSalutationAdvancedTabCompleter\""));
    }

    @Test
    void portedFeatureMixinsAreInTheRequiredEarlyConfig() {
        String config = config("mixins.neofontrender_ui_enhancements.json");

        assertTrue(config.contains("\"MixinGuiLanguageResourceReload\""));
        assertTrue(config.contains("\"MixinGuiLanguageSearch\""));
        assertTrue(config.contains("\"MixinGuiLanguageListFavorites\""));
        assertTrue(config.contains("\"MixinGuiSlotLanguageFavorites\""));
        assertTrue(config.contains("\"MixinGuiCreateWorldModernLayout\""));
        assertTrue(config.contains("\"MixinGuiButtonCreateWorldStyle\""));
        assertTrue(config.contains("\"GuiCreateWorldAccessor\""));
        assertTrue(config.contains("\"MixinGuiMainMenuContinueGame\""));
        assertTrue(config.contains("\"MixinEntityRendererZoomMouse\""));
        assertTrue(config.contains("\"MixinEntityRendererMouseInputEvent\""));
        assertTrue(config.contains("\"MixinRenderPlayerFlightRoll\""));
        assertTrue(config.contains("\"MixinGuiIngameForgeCrosshair\""));
        assertTrue(config.contains("\"AccessorGuiChatFeatures\""));
        assertTrue(config.contains("\"AccessorGuiNewChatFeatures\""));
        assertTrue(config.contains("\"MixinChatLineMetadata\""));
        assertTrue(config.contains("\"MixinGuiNewChatFeatures\""));
        assertTrue(config.contains("\"MixinGuiScreenResourcePacksProgress\""));
        assertTrue(config.contains("\"MixinGuiScreenBookCjkTypography\""));
        assertTrue(config.contains("\"MixinProgressBarResourceReload\""));
        assertTrue(config.contains("\"MixinProgressManagerResourceReload\""));
        assertTrue(config.contains("\"MixinMinecraftResourceReloadProgress\""));
        assertTrue(config.contains("\"MixinTextureManagerResourceReloadPulse\""));
    }

    @Test
    void shoulderSurfingCompatMixinsAreLateAndNonRequired() {
        String shoulderSurfing = config(
                "mixins.neofontrender_ui_enhancements_shouldersurfing.json");
        String shoulderSurfingTconstruct = config(
                "mixins.neofontrender_ui_enhancements_shouldersurfing_tconstruct.json");
        String shoulderSurfingMatterOverdrive = config(
                "mixins.neofontrender_ui_enhancements_shouldersurfing_matteroverdrive.json");

        assertTrue(shoulderSurfing.contains("\"required\": false"));
        assertTrue(shoulderSurfing.contains(
                "\"compat.MixinShoulderSurfingCrosshairMatrix\""));
        assertTrue(shoulderSurfing.contains(
                "\"compat.MixinEntityRendererShoulderSurfingMouseOver\""));
        assertTrue(shoulderSurfingTconstruct.contains("\"required\": false"));
        assertTrue(shoulderSurfingTconstruct.contains(
                "\"compat.MixinTConstructCrosshairOffset\""));
        assertTrue(shoulderSurfingMatterOverdrive.contains("\"required\": false"));
        assertTrue(shoulderSurfingMatterOverdrive.contains(
                "\"compat.MixinMatterOverdriveCrosshairOffset\""));
    }

    @Test
    void zoomMixinLeavesEntityPlayerTurnAvailableToOtherCoremods() {
        InputStream stream = UiEnhancementsMixinConfigTest.class.getClassLoader().getResourceAsStream(
                "neofontrender/addons/mixin/MixinEntityRendererZoomMouse.class");
        assertNotNull(stream);
        try (InputStream input = stream) {
            String bytecode = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertFalse(bytecode.contains("net/minecraft/client/entity/EntityPlayerSP"));
        } catch (Exception error) {
            throw new AssertionError("Failed to inspect zoom mixin bytecode", error);
        }
    }

    @Test
    void earlyConfigHasNoJeiOrHeiHoverMixins() {
        String config = config("mixins.neofontrender_ui_enhancements.json");

        // 1.7.10 has neither JEI nor HEI; their hover mixins and the HEI late config must stay absent.
        assertFalse(config.contains("MixinJei"));
        assertFalse(config.contains("MixinHei"));
        assertNull(UiEnhancementsMixinConfigTest.class.getClassLoader()
                .getResourceAsStream("mixins.neofontrender_ui_enhancements_hei.json"));
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
