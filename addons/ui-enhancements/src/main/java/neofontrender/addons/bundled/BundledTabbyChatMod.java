package neofontrender.addons.bundled;

import cpw.mods.fml.common.Mod;
import neofontrender.addons.ui.NfrUiEnhancements;

/** ModList metadata for the embedded TabbyChat 2 Reforged sources. */
@Mod(modid = "tabbychat2", name = "TabbyChat 2 Reforged (UIE Embedded)", version = "2.0.0-uie",
        modLanguage = "java", acceptableRemoteVersions = "*",
        dependencies = "required-after:" + NfrUiEnhancements.MOD_ID + "@[" + NfrUiEnhancements.VERSION + ",)",
        guiFactory = "neofontrender.addons.bundled.BundledTabbyChatGuiFactory")
public final class BundledTabbyChatMod {
    public BundledTabbyChatMod() {
        BundledModRegistry.markTabbyChat();
    }
}
