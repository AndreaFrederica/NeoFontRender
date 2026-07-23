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

    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList(HEI_CONFIG, OBSCURE_TOOLTIPS_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(Context context) {
        String config = context.mixinConfig();
        if (HEI_CONFIG.equals(config)) {
            return context.isModPresent("jei")
                    && classResourcePresent("mezz/jei/gui/TooltipRenderer.class")
                    && classResourcePresent("mezz/jei/render/CollapsedGroupRenderer.class");
        }
        if (OBSCURE_TOOLTIPS_CONFIG.equals(config)) {
            return classResourcePresent("dev/obscuria/tooltips/client/TooltipState.class")
                    && classResourcePresent("dev/obscuria/tooltips/client/component/HeaderComponent.class");
        }
        return false;
    }

    private static boolean classResourcePresent(String resource) {
        return Launch.classLoader != null && Launch.classLoader.getResource(resource) != null;
    }
}
