package neofontrender.addons.compat;

import neofontrender.client.NeofontrenderFancyMenu;
import neofontrender.core.config.NeofontrenderConfig;

import java.util.Arrays;
import java.util.List;

/** FancyMenu 2.x edits the vanilla continue button through its normal button cache. */
public final class FancyMenuCompat implements ModCompat {
    @Override public String id() { return NeofontrenderFancyMenu.MOD_ID; }
    @Override public String displayName() { return "FancyMenu"; }
    @Override public boolean isActive() { return NeofontrenderFancyMenu.present(); }

    @Override public List<CompatImpact> impacts() {
        String prefix = "neofontrender_ui_enhancements.";
        return Arrays.asList(
                new CompatImpact(CustomMainMenuCompat.KIND_SUSPENDED, prefix + "gui.diagnostics.feature.nova",
                        prefix + "compat.fancymenu." + (NeofontrenderConfig.compatFancyMenu()
                                ? "nova_kept" : "nova_suspended")),
                new CompatImpact(CustomMainMenuCompat.KIND_SUSPENDED,
                        prefix + "gui.diagnostics.feature.main_menu_label", prefix + "compat.fancymenu.menu"));
    }
}
