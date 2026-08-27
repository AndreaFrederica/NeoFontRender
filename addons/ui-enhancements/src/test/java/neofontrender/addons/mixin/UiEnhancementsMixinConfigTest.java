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
        String shoulderSurfing = config(
                "mixins.neofontrender_ui_enhancements_shouldersurfing.json");
        String shoulderSurfingTconstruct = config(
                "mixins.neofontrender_ui_enhancements_shouldersurfing_tconstruct.json");
        String shoulderSurfingMatterOverdrive = config(
                "mixins.neofontrender_ui_enhancements_shouldersurfing_matteroverdrive.json");
        String betterCombat = config(
                "mixins.neofontrender_ui_enhancements_bettercombat.json");

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
        assertTrue(betterCombat.contains("\"required\": false"));
        assertTrue(betterCombat.contains(
                "\"compat.MixinBetterCombatShoulderCrosshair\""));
    }

    @Test
    void resourceReloadMixinsAreInTheRequiredEarlyConfig() {
        String config = config("mixins.neofontrender_ui_enhancements.json");

        assertTrue(config.contains("\"MixinCommandHandlerCompletionProviders\""));
        assertTrue(config.contains("\"MixinGuiLanguageResourceReload\""));
        assertTrue(config.contains("\"MixinLocaleAddonTranslations\""));
        assertTrue(config.contains("\"MixinGuiLanguageSearch\""));
        assertTrue(config.contains("\"MixinGuiLanguageListFavorites\""));
        assertTrue(config.contains("\"MixinGuiSlotLanguageFavorites\""));
        assertTrue(config.contains("\"MixinGuiCreateWorldModularBridge\""));
        assertTrue(config.contains("\"MixinGuiButtonCreateWorldStyle\""));
        assertTrue(config.contains("\"GuiCreateWorldAccessor\""));
        assertTrue(config.contains("\"MixinGuiMainMenuContinueGame\""));
        assertTrue(config.contains("\"MixinEntityRendererZoomMouse\""));
        assertTrue(config.contains("\"MixinEntityRendererMouseInputEvent\""));
        assertTrue(config.contains("\"MixinEntityRendererUiNavigationPointer\""));
        assertTrue(config.contains("\"MixinGuiScreenSyntheticMouseInput\""));
        assertTrue(config.contains("\"MixinEntityRendererCameraPresentation\""));
        assertTrue(config.contains("\"AccessorEntityRendererCameraDistance\""));
        assertTrue(config.contains("\"MixinMinecraftPerspectiveCycle\""));
        assertTrue(config.contains("\"MixinMinecraftDroneInputGate\""));
        assertTrue(config.contains("\"MixinMovementInputFromOptionsDroneGate\""));
        assertTrue(config.contains("\"MixinRenderPlayerCameraTransparency\""));
        assertTrue(config.contains("\"MixinRenderLivingBaseCameraViewIdentity\""));
        assertTrue(config.contains("\"MixinGlStateManagerCameraTransparency\""));
        assertTrue(config.contains("\"MixinRenderPlayerFlightRoll\""));
        assertTrue(config.contains("\"MixinGuiIngameForgeCrosshair\""));
        assertTrue(config.contains("\"InvokerGuiIngameCrosshair\""));
        assertTrue(config.contains("\"AccessorGuiChatFeatures\""));
        assertTrue(config.contains("\"AccessorGuiNewChatFeatures\""));
        assertTrue(config.contains("\"MixinChatLineMetadata\""));
        assertTrue(config.contains("\"MixinGuiNewChatFeatures\""));
        assertTrue(config.contains("\"MixinGuiButtonHover\""));
        assertTrue(config.contains("\"MixinGuiButtonExtHover\""));
        assertTrue(config.contains("\"MixinForgeGuiUtilsButtonAlpha\""));
        assertTrue(config.contains("\"MixinForgeGuiConfirmationModern\""));
        assertTrue(config.contains("\"MixinForgeGuiNotificationModern\""));
        assertTrue(config.contains("\"MixinGuiContainerSlotHover\""));
        assertTrue(config.contains("\"AccessorGuiScreenNavigation\""));
        assertTrue(config.contains("\"AccessorGuiSlotNavigation\""));
        assertTrue(config.contains("\"AccessorGuiScrollingListNavigation\""));
        assertTrue(config.contains("\"AccessorGuiKeyBindingEntryNavigation\""));
        assertTrue(config.contains("\"AccessorGuiTextFieldNavigation\""));
        assertTrue(config.contains("\"AccessorGuiOptionSliderNavigation\""));
        assertTrue(config.contains("\"MixinGuiListExtendedNavigationCapture\""));
        assertTrue(config.contains("\"MixinCustomGuiButtonNavigationCapture\""));
        assertTrue(config.contains("\"MixinModularItemSlotHover\""));
        assertTrue(config.contains("\"MixinGuiScreenResourcePacksProgress\""));
        assertTrue(config.contains("\"MixinGuiScreenBookCjkTypography\""));
        assertTrue(config.contains("\"MixinProgressBarResourceReload\""));
        assertTrue(config.contains("\"MixinProgressManagerResourceReload\""));
        assertTrue(config.contains("\"MixinViewFrustumLoadingProgress\""));
    }

    @Test
    void movementInputStateIsAccessedOnItsDeclaringSuperclass() {
        String config = config("mixins.neofontrender_ui_enhancements.json");
        String gateBytecode = bytecode(
                "neofontrender/addons/mixin/MixinMovementInputFromOptionsDroneGate.class");
        String accessorBytecode = bytecode(
                "neofontrender/addons/mixin/AccessorMovementInputState.class");

        assertTrue(config.contains("\"AccessorMovementInputState\""));
        assertFalse(gateBytecode.contains("Lorg/spongepowered/asm/mixin/Shadow;"));
        assertTrue(accessorBytecode.contains("net/minecraft/util/MovementInput"));
    }

    @Test
    void cameraPresentationUsesRequiredTargetedInjections() {
        String bytecode = bytecode(
                "neofontrender/addons/mixin/MixinEntityRendererCameraPresentation.class");

        assertTrue(bytecode.contains("orientCamera"));
        assertTrue(bytecode.contains("thirdPersonDistancePrev"));
        assertTrue(bytecode.contains("suppressesVanillaThirdPersonDisplacement"));
        assertTrue(bytecode.contains("Lorg/spongepowered/asm/mixin/injection/Redirect;"));
        assertTrue(bytecode.contains("Lorg/spongepowered/asm/mixin/injection/ModifyConstant;"));
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
    void controllerLookRoutingRemainsInTheMouseBoundary() {
        String bytecode = bytecode(
                "neofontrender/addons/mixin/MixinEntityRendererMouseInputEvent.class");

        assertTrue(bytecode.contains("CAMERA_LOOK_X"));
        assertTrue(bytecode.contains("CAMERA_LOOK_Y"));
        assertTrue(bytecode.contains("FlightApi"));
        assertTrue(bytecode.contains("resolveCameraDelta"));
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

    private static String bytecode(String name) {
        InputStream stream = UiEnhancementsMixinConfigTest.class.getClassLoader()
                .getResourceAsStream(name);
        assertNotNull(stream, name);
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        } catch (Exception error) {
            throw new AssertionError("Failed to read " + name, error);
        }
    }
}
