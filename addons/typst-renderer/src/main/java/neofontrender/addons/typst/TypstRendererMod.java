package neofontrender.addons.typst;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import neofontrender.addons.hud.compositor.HudWindowCompositor;

/** Optional Typst inline-rendering addon. */
@Mod(modid = TypstRendererMod.MOD_ID, name = TypstRendererMod.MOD_NAME,
        version = TypstRendererMod.VERSION, dependencies = TypstRendererMod.DEPENDENCIES,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.12,1.13)")
public final class TypstRendererMod {
    public static final String MOD_ID = "neofontrender_typst_renderer";
    public static final String MOD_NAME = "NFR Typst Renderer";
    public static final String VERSION = "0.1.0";
    public static final String DEPENDENCIES =
            "required-after:neofontrender@[0.6.0,)";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        TypstConfig.load();
        TypstLaboratorySettings.register();
        TypstMiddleware.initialize();
        neofontrender.api.client.settings.NfrSettingsPageRegistry.register(new TypstPackagesSettings());
        HudWindowCompositor.INSTANCE.register(TypstStatus.INSTANCE);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        TypstMiddleware.initializeShutdownHook();
    }
}
