package neofontrender.addons.bundled;

import cpw.mods.fml.common.Mod;
import neofontrender.addons.ui.NfrUiEnhancements;

/** ModList metadata for the embedded Salutation 1.12.2 sources. */
@Mod(modid = "salutation", name = "Salutation (UIE Embedded)", version = "1.0.0-uie",
        modLanguage = "java", acceptableRemoteVersions = "*",
        dependencies = "required-after:" + NfrUiEnhancements.MOD_ID + "@[" + NfrUiEnhancements.VERSION + ",)",
        guiFactory = "neofontrender.addons.bundled.BundledSalutationGuiFactory")
public final class BundledSalutationMod {
    public BundledSalutationMod() {
        BundledModRegistry.markSalutation();
    }
}
