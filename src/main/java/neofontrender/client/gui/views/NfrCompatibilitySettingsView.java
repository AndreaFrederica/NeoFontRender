package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Compatibility fixes for third-party mods. */
public final class NfrCompatibilitySettingsView extends NfrContentView<NfrCompatibilitySettingsView> {
    public NfrCompatibilitySettingsView(NfrSettingsDraft d, NfrSettingsControls c) { this(options(d, c)); }

    private NfrCompatibilitySettingsView(NfrOptionsGrid options) {
        super(section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c) {
        return c.grid()
                .add(c.toggle("neofontrender.gui.option.compat_tinkers_antique",
                        "neofontrender.tooltip.compat_tinkers_antique",
                        () -> d.compatTinkersAntique, value -> d.compatTinkersAntique = value));
    }
}
