package neofontrender.addons.compat;

import neofontrender.client.NeofontrenderCustomMainMenu;
import neofontrender.core.config.NeofontrenderConfig;

import java.util.ArrayList;
import java.util.List;

/** Reports CMM's global branding policy and title-screen behavior without disabling mixins. */
public final class CustomMainMenuCompat implements ModCompat {

    /** Impact category for a feature voluntarily suppressed under another mod's takeover. */
    public static final String KIND_SUSPENDED = "suspended_feature";

    private static final String FEATURE_NOVA = "neofontrender_ui_enhancements.gui.diagnostics.feature.nova";
    private static final String FEATURE_LABEL =
            "neofontrender_ui_enhancements.gui.diagnostics.feature.main_menu_label";

    @Override
    public String id() {
        return NeofontrenderCustomMainMenu.MOD_ID;
    }

    @Override
    public String displayName() {
        return "Custom Main Menu";
    }

    @Override
    public boolean isActive() {
        return NeofontrenderCustomMainMenu.present();
    }

    @Override
    public List<CompatImpact> impacts() {
        List<CompatImpact> impacts = new ArrayList<>();
        if (NeofontrenderConfig.compatCustomMainMenu()) {
            impacts.add(new CompatImpact(KIND_SUSPENDED, FEATURE_NOVA,
                    "neofontrender_ui_enhancements.compat.custommainmenu.nova_kept"));
        } else {
            impacts.add(new CompatImpact(KIND_SUSPENDED, FEATURE_NOVA,
                "neofontrender_ui_enhancements.compat.custommainmenu.nova_suspended"));
        }
        impacts.add(new CompatImpact(KIND_SUSPENDED, FEATURE_LABEL,
                "neofontrender_ui_enhancements.compat.custommainmenu.label_suspended"));
        return impacts;
    }
}
