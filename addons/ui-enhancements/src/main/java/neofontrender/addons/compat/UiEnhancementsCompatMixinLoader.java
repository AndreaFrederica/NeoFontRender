package neofontrender.addons.compat;

import net.minecraft.launchwrapper.Launch;
import zone.rong.mixinbooter.Context;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.List;

/**
 * Queues optional integrations after Forge adds mod jars to the classpath. This class deliberately
 * lives outside the configured Mixin package, whose classes LaunchWrapper is forbidden to load.
 */
public final class UiEnhancementsCompatMixinLoader implements ILateMixinLoader {
    static final String HEI_CONFIG = "mixins.neofontrender_ui_enhancements_hei.json";
    static final String OBSCURE_TOOLTIPS_CONFIG =
            "mixins.neofontrender_ui_enhancements_obscure_tooltips.json";
    static final String SALUTATION_CONFIG =
            "mixins.neofontrender_ui_enhancements_salutation.json";
    static final String QUARK_CONFIG =
            "mixins.neofontrender_ui_enhancements_quark.json";
    static final String SHOULDER_SURFING_CONFIG =
            "mixins.neofontrender_ui_enhancements_shouldersurfing.json";
    static final String SHOULDER_SURFING_TCONSTRUCT_CONFIG =
            "mixins.neofontrender_ui_enhancements_shouldersurfing_tconstruct.json";
    static final String SHOULDER_SURFING_MATTER_OVERDRIVE_CONFIG =
            "mixins.neofontrender_ui_enhancements_shouldersurfing_matteroverdrive.json";

    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList(HEI_CONFIG, OBSCURE_TOOLTIPS_CONFIG, SALUTATION_CONFIG, QUARK_CONFIG,
                SHOULDER_SURFING_CONFIG, SHOULDER_SURFING_TCONSTRUCT_CONFIG,
                SHOULDER_SURFING_MATTER_OVERDRIVE_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(Context context) {
        String config = context.mixinConfig();
        if (HEI_CONFIG.equals(config)) {
            return context.isModPresent("jei")
                    && classResourcePresent("mezz/jei/gui/TooltipRenderer.class")
                    && classResourcePresent("mezz/jei/gui/overlay/IngredientGrid.class")
                    && classResourcePresent("mezz/jei/render/IngredientRenderer.class");
        }
        if (OBSCURE_TOOLTIPS_CONFIG.equals(config)) {
            return classResourcePresent("dev/obscuria/tooltips/client/TooltipState.class")
                    && classResourcePresent("dev/obscuria/tooltips/client/component/HeaderComponent.class");
        }
        if (SALUTATION_CONFIG.equals(config)) {
            return context.isModPresent("salutation")
                    && classResourcePresent(
                            "speiger/src/salutation/client/ClientHandler.class")
                    && classResourcePresent(
                            "speiger/src/salutation/client/gui/chat/AdvancedTabCompleter.class")
                    && classResourcePresent(
                            "speiger/src/salutation/client/gui/chat/ChatScreen.class");
        }
        if (QUARK_CONFIG.equals(config)) {
            return context.isModPresent("quark")
                    && classResourcePresent("vazkii/quark/client/feature/MapTooltip.class");
        }
        if (SHOULDER_SURFING_CONFIG.equals(config)) {
            return context.isModPresent("shouldersurfing")
                    && classResourcePresent(
                            "com/teamderpy/shouldersurfing/client/ShoulderRenderer.class");
        }
        if (SHOULDER_SURFING_TCONSTRUCT_CONFIG.equals(config)) {
            return context.isModPresent("shouldersurfing") && context.isModPresent("tconstruct")
                    && classResourcePresent(
                            "com/teamderpy/shouldersurfing/client/ShoulderRenderer.class")
                    && classResourcePresent(
                            "slimeknights/tconstruct/library/client/crosshair/CrosshairRenderEvents.class");
        }
        if (SHOULDER_SURFING_MATTER_OVERDRIVE_CONFIG.equals(config)) {
            return context.isModPresent("shouldersurfing")
                    && context.isModPresent("matteroverdrive")
                    && classResourcePresent(
                            "com/teamderpy/shouldersurfing/client/ShoulderRenderer.class")
                    && classResourcePresent("matteroverdrive/gui/GuiAndroidHud.class");
        }
        return false;
    }

    private static boolean classResourcePresent(String resource) {
        return Launch.classLoader != null && Launch.classLoader.getResource(resource) != null;
    }
}
