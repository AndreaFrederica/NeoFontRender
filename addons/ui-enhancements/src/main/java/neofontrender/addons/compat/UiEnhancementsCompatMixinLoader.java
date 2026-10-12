package neofontrender.addons.compat;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import net.minecraft.launchwrapper.Launch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Queues optional integration mixins after Forge adds mod jars to the classpath, through the
 * GTNHMixins late-loader contract (ASMData scan of {@link LateMixin}). This class deliberately
 * lives outside the configured Mixin package, whose classes LaunchWrapper is forbidden to load.
 */
@LateMixin
public final class UiEnhancementsCompatMixinLoader implements ILateMixinLoader {
    static final String LATE_CONFIG = "mixins.neofontrender_ui_enhancements_late.json";

    @Override
    public String getMixinConfig() {
        return LATE_CONFIG;
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>();
        // Salutation is bundled inside this jar, so its targets are always present.
        mixins.add("compat.MixinSalutationClientHandler");
        mixins.add("compat.MixinSalutationChatScreen");
        if (loadedMods.contains("shouldersurfing")
                && classResourcePresent("com/teamderpy/shouldersurfing/client/ShoulderRenderer.class")) {
            mixins.add("compat.MixinShoulderSurfingCrosshairMatrix");
            mixins.add("compat.MixinEntityRendererShoulderSurfingMouseOver");
        }
        if (loadedMods.contains("TConstruct")
                && classResourcePresent(
                        "slimeknights/tconstruct/library/client/crosshair/CrosshairRenderEvents.class")) {
            mixins.add("compat.MixinTConstructCrosshairOffset");
        }
        if (loadedMods.contains("mo")
                && classResourcePresent("matteroverdrive/gui/GuiAndroidHud.class")) {
            mixins.add("compat.MixinMatterOverdriveCrosshairOffset");
        }
        return mixins;
    }

    private static boolean classResourcePresent(String resource) {
        return Launch.classLoader != null && Launch.classLoader.getResource(resource) != null;
    }
}
