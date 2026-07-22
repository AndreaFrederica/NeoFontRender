package neofontrender.client.gui.views;

import net.minecraft.client.resources.I18n;
import neofontrender.client.gui.component.base.NfrTextInfoPanel;
import neofontrender.api.client.settings.NfrInfoLine;
import neofontrender.api.client.settings.NfrInfoPage;
import neofontrender.api.client.settings.NfrInfoPageContribution;
import neofontrender.api.client.settings.NfrInfoPageRegistry;

import java.util.ArrayList;
import java.util.List;

/** Third-party licenses route. */
public final class NfrLicensesSettingsView extends NfrContentView<NfrLicensesSettingsView> {
    public NfrLicensesSettingsView() {
        this(content());
    }

    private NfrLicensesSettingsView(NfrTextInfoPanel content) {
        super(section(content, width -> content.preferredHeight()));
    }

    private static NfrTextInfoPanel content() {
        List<NfrTextInfoPanel.Line> lines = new ArrayList<>();
        lines.add(NfrTextInfoPanel.line(tr("neofontrender.gui.licenses.title"), 0xFFFFFF));
        lines.add(NfrTextInfoPanel.spaced("cosmic-text - MIT / Apache-2.0", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("Skija - Apache-2.0", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("Skia - BSD-3-Clause", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("Arc3D Core - LGPL-3.0-or-later", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("NightConfig - LGPL-3.0", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("ModularUI - LGPL-3.0", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("LWJGL / LWJGLX - BSD-3-Clause", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("SpongePowered Mixin - MIT", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("Sarasa Gothic - SIL OFL-1.1", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("Noto Color Emoji - SIL OFL-1.1", 0xD8D8D8));
        appendContributions(lines);
        lines.add(NfrTextInfoPanel.spaced(tr("neofontrender.gui.licenses.notice"), 0xBFC7D1));
        return new NfrTextInfoPanel(lines);
    }

    private static void appendContributions(List<NfrTextInfoPanel.Line> target) {
        for (NfrInfoPageContribution contribution : NfrInfoPageRegistry.snapshot(NfrInfoPage.LICENSES)) {
            List<NfrInfoLine> contributed = contribution.lines();
            if (contributed == null) continue;
            for (NfrInfoLine line : contributed) {
                if (line != null) target.add(NfrTextInfoPanel.line(line.text(), line.color(), line.gapBefore()));
            }
        }
    }

    private static String tr(String key) {
        return I18n.format(key);
    }
}
