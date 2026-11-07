package neofontrender.addons.inlinecontent;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import neofontrender.addons.inlinecontent.client.InlineContentShowcaseClient;

@Mod(
        modid = InlineContentShowcaseMod.MOD_ID,
        name = InlineContentShowcaseMod.MOD_NAME,
        version = InlineContentShowcaseMod.VERSION,
        dependencies = "required-after:neofontrender;required-after:neofontrender_ui_enhancements",
        acceptedMinecraftVersions = "[1.12,1.13)",
        clientSideOnly = true
)
public final class InlineContentShowcaseMod {
    public static final String MOD_ID = "neofontrender_inline_content_showcase";
    public static final String MOD_NAME = "NFR Inline Content Showcase";
    public static final String VERSION = "0.1.0";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        InlineContentShowcaseClient.init();
    }
}
