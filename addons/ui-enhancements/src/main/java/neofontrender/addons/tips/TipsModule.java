package neofontrender.addons.tips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

/**
 * Tips module: displays rotating tips on loading screens.
 * Seamless replacement for the Tips mod: registers the same resource paths
 * so existing tip resource packs continue to work.
 */
public final class TipsModule implements UiEnhancementModule {
    /** Our built-in tip file. */
    static final ResourceLocation BUILTIN_TIPS =
            new ResourceLocation("neofontrender_ui_enhancements", "tips/tips.json");

    /** Tips mod's default tip path — registered for compatibility with existing resource packs. */
    static final ResourceLocation TIPSMOD_TIPS =
            new ResourceLocation("tipsmod", "tips/tips.json");

    @Override
    public void preInit() {
        TipsConfig.load();
        // Register both paths so existing Tips mod resource packs work
        TipManager.INSTANCE.registerTipFile(BUILTIN_TIPS);
        TipManager.INSTANCE.registerTipFile(TIPSMOD_TIPS);
    }

    @Override
    public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) mc.getResourceManager())
                    .registerReloadListener(TipManager.INSTANCE);
        }
        NfrSettingsPageRegistry.register(new TipsSettingsPage());
    }
}
