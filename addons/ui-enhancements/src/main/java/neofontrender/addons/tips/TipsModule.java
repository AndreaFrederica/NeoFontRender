package neofontrender.addons.tips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.ui.UiEnhancementModule;

/**
 * Tips module: displays rotating tips on loading screens.
 * Tips are loaded from resource pack JSON files, allowing any mod to contribute.
 */
public final class TipsModule implements UiEnhancementModule {
    static final ResourceLocation BUILTIN_TIPS =
            new ResourceLocation("neofontrender_ui_enhancements", "tips/tips.json");

    @Override
    public void preInit() {
        TipsConfig.load();
        TipManager.INSTANCE.registerTipFile(BUILTIN_TIPS);
    }

    @Override
    public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.getResourceManager() instanceof SimpleReloadableResourceManager) {
            ((SimpleReloadableResourceManager) mc.getResourceManager())
                    .registerReloadListener(TipManager.INSTANCE);
        }
    }
}
