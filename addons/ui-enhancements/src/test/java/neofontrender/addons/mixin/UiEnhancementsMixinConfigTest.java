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
        String salutation = config("mixins.neofontrender_ui_enhancements_salutation.json");
        String quark = config("mixins.neofontrender_ui_enhancements_quark.json");

        assertTrue(hei.contains("\"required\": false"));
        assertTrue(hei.contains("\"compat.MixinJeiIngredientRendererHover\""));
        assertTrue(hei.contains("\"compat.MixinHeiCollapsedGroupHover\""));
        assertTrue(hei.contains("\"compat.MixinJeiIngredientGridHover\""));
        assertTrue(hei.contains("\"compat.MixinHeiTooltipRenderer\""));
        assertTrue(hei.contains("\"compat.MixinHeiCollapsedGroupTooltip\""));
        assertTrue(obscure.contains("\"required\": false"));
        assertTrue(obscure.contains("\"compat.MixinObscureHeaderComponent\""));
        assertTrue(obscure.contains("\"compat.MixinObscureTooltipState\""));
        assertTrue(salutation.contains("\"required\": false"));
        assertTrue(salutation.contains("\"compat.MixinSalutationAdvancedTabCompleter\""));
        assertTrue(quark.contains("\"required\": false"));
        assertTrue(quark.contains("\"compat.MixinQuarkMapTooltip\""));
    }

    @Test
    void resourceReloadMixinsAreInTheRequiredEarlyConfig() {
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
        assertTrue(config.contains("\"MixinGuiButtonHover\""));
        assertTrue(config.contains("\"MixinGuiButtonExtHover\""));
        assertTrue(config.contains("\"MixinForgeGuiUtilsButtonAlpha\""));
        assertTrue(config.contains("\"MixinGuiContainerSlotHover\""));
        assertTrue(config.contains("\"MixinModularItemSlotHover\""));
        assertTrue(config.contains("\"MixinGuiScreenResourcePacksProgress\""));
        assertTrue(config.contains("\"MixinGuiScreenBookCjkTypography\""));
        assertTrue(config.contains("\"MixinProgressBarResourceReload\""));
        assertTrue(config.contains("\"MixinProgressManagerResourceReload\""));
        assertTrue(config.contains("\"MixinViewFrustumLoadingProgress\""));
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
