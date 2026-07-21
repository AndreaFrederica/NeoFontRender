package neofontrender.client.gui.views;

import net.minecraft.client.resources.I18n;
import neofontrender.client.gui.component.base.NfrTextInfoPanel;

/** Third-party licenses route. */
public final class NfrLicensesSettingsView extends NfrContentView<NfrLicensesSettingsView> {
    public NfrLicensesSettingsView() {
        this(content());
    }

    private NfrLicensesSettingsView(NfrTextInfoPanel content) {
        super(section(content, width -> content.preferredHeight()));
    }

    private static NfrTextInfoPanel content() {
        return new NfrTextInfoPanel(
                NfrTextInfoPanel.line(tr("neofontrender.gui.licenses.title"), 0xFFFFFF),
                NfrTextInfoPanel.spaced("cosmic-text - MIT / Apache-2.0", 0xD8D8D8),
                NfrTextInfoPanel.line("Skija / Skia - Apache-2.0 / BSD-3-Clause", 0xD8D8D8),
                NfrTextInfoPanel.line("ModularUI - LGPL-3.0", 0xD8D8D8),
                NfrTextInfoPanel.line("LWJGL - BSD-3-Clause", 0xD8D8D8),
                NfrTextInfoPanel.line("Sarasa Gothic - SIL OFL-1.1", 0xD8D8D8),
                NfrTextInfoPanel.line("Noto Color Emoji - SIL OFL-1.1", 0xD8D8D8),
                NfrTextInfoPanel.spaced(tr("neofontrender.gui.licenses.notice"), 0xBFC7D1));
    }

    private static String tr(String key) {
        return I18n.format(key);
    }
}
