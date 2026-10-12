package neofontrender.addons.mixin;

import neofontrender.addons.compat.UiEnhancementsCompatMixinLoader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiEnhancementsMixinConfigTest {
    @Test
    void earlyConfigKeepsPlatformCompatMixinsWithoutUnavailableModClasses() {
        String config = config("mixins.neofontrender_ui_enhancements.json");

        // 1.7.10 ships NEI instead of JEI/HEI, and the NEI, Salutation and TabbyChat bridges have no
        // separate late config here, so the non-required early config keeps them as pseudo mixins.
        assertFalse(config.contains("\"MixinJei"));
        assertFalse(config.contains("\"MixinHei"));
        assertTrue(config.contains("\"compat.MixinNeiIngredientGridHover\""));
        assertTrue(config.contains("\"compat.MixinSalutationAdvancedTabCompleter\""));
    }

    @Test
    void startupDoesNotReferenceTheObsoleteTextDependencySynchronizer() {
        String loader = bytecode(
                "neofontrender/addons/mixin/UiEnhancementsMixinLoader.class");

        assertFalse(loader.contains(
                "ContainedDependencySynchronizer"));
    }

    @Test
    void optionalCompatMixinsAreQueuedByTheLateLoader() {
        String lateConfig = config("mixins.neofontrender_ui_enhancements_late.json");
        assertTrue(lateConfig.contains("\"required\": false"));

        UiEnhancementsCompatMixinLoader loader = new UiEnhancementsCompatMixinLoader();
        assertEquals("mixins.neofontrender_ui_enhancements_late.json", loader.getMixinConfig());

        // Bundled Salutation mixins are always queued. Mod-gated mixins additionally require
        // their target classes on the launch classpath, which the unit-test runtime lacks.
        List<String> withNothing = loader.getMixins(Collections.emptySet());
        assertTrue(withNothing.contains("compat.MixinSalutationClientHandler"));
        assertTrue(withNothing.contains("compat.MixinSalutationChatScreen"));
        assertFalse(withNothing.contains("compat.MixinShoulderSurfingCrosshairMatrix"));
        assertFalse(withNothing.contains("compat.MixinTConstructCrosshairOffset"));
        assertFalse(withNothing.contains("compat.MixinMatterOverdriveCrosshairOffset"));

        List<String> withShoulderSurfing = loader.getMixins(Collections.singleton("shouldersurfing"));
        assertFalse(withShoulderSurfing.contains("compat.MixinShoulderSurfingCrosshairMatrix"));
    }

    @Test
    void resourceReloadMixinsAreInTheRequiredEarlyConfig() {
        String config = config("mixins.neofontrender_ui_enhancements.json");

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
        assertTrue(config.contains("\"MixinMinecraftPerspectiveCycle\""));
        assertTrue(config.contains("\"MixinMinecraftDroneInputGate\""));
        assertTrue(config.contains("\"MixinMovementInputFromOptionsDroneGate\""));
        assertTrue(config.contains("\"MixinRenderPlayerFlightRoll\""));
        assertFalse(config.contains("\"MixinGuiBossOverlayFlightHud\""));
        assertFalse(config.contains("\"MixinGuiIngameForgeFlightHud\""));
        assertTrue(config.contains("\"MixinGuiIngameForgeCrosshair\""));
        assertTrue(config.contains("\"AccessorGuiChatFeatures\""));
        assertTrue(config.contains("\"AccessorGuiNewChatFeatures\""));
        assertTrue(config.contains("\"MixinChatLineMetadata\""));
        assertTrue(config.contains("\"MixinGuiNewChatFeatures\""));
        assertTrue(config.contains("\"MixinGuiButtonHover\""));
        assertTrue(config.contains("\"MixinGuiButtonCursor\""));
        assertTrue(config.contains("\"MixinGuiButtonExtHover\""));
        assertTrue(config.contains("\"MixinForgeGuiUtilsButtonAlpha\""));
        assertTrue(config.contains("\"MixinGuiContainerSlotHover\""));
        assertTrue(config.contains("\"AccessorGuiScreenNavigation\""));
        assertTrue(config.contains("\"AccessorGuiSlotNavigation\""));
        assertTrue(config.contains("\"AccessorGuiScrollingListNavigation\""));
        assertTrue(config.contains("\"AccessorGuiKeyBindingEntryNavigation\""));
        assertTrue(config.contains("\"AccessorGuiTextFieldNavigation\""));
        assertTrue(config.contains("\"AccessorGuiOptionSliderNavigation\""));
        assertTrue(config.contains("\"MixinGuiListExtendedNavigationCapture\""));
        assertTrue(config.contains("\"MixinModularItemSlotHover\""));
        assertTrue(config.contains("\"MixinGuiScreenResourcePacksProgress\""));
        assertTrue(config.contains("\"MixinGuiScreenBookCjkTypography\""));
        assertTrue(config.contains("\"MixinProgressBarResourceReload\""));
        assertTrue(config.contains("\"MixinProgressManagerResourceReload\""));
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
    void cameraPresentationModifiesTheComputedDistance() {
        String bytecode = bytecode(
                "neofontrender/addons/mixin/MixinEntityRendererCameraPresentation.class");

        assertTrue(bytecode.contains("orientCamera"));
        assertTrue(bytecode.contains("suppressesVanillaThirdPersonDisplacement"));
        assertTrue(bytecode.contains("Lorg/spongepowered/asm/mixin/injection/ModifyVariable;"));
        assertFalse(bytecode.contains("Lorg/spongepowered/asm/mixin/injection/Redirect;"));
        assertFalse(bytecode.contains("Lorg/spongepowered/asm/mixin/injection/ModifyConstant;"));
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
