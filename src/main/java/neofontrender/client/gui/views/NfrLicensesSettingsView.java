package neofontrender.client.gui.views;

import net.minecraft.client.resources.I18n;
import neofontrender.client.gui.component.base.NfrTextInfoPanel;
import neofontrender.api.client.settings.NfrInfoLine;
import neofontrender.api.client.settings.NfrInfoPage;
import neofontrender.api.client.settings.NfrInfoPageContribution;
import neofontrender.api.client.settings.NfrInfoPageRegistry;
import neofontrender.client.licenses.ThirdPartyLicenseCatalog;

import java.util.ArrayList;
import java.util.List;

/** Third-party licenses route. */
public final class NfrLicensesSettingsView extends NfrContentView<NfrLicensesSettingsView> {
    public NfrLicensesSettingsView() {
        this(content());
    }

    private NfrLicensesSettingsView(NfrTextInfoPanel content) {
        super(section(content, width -> content.preferredHeight(Math.max(0, width - 6))));
    }

    private static NfrTextInfoPanel content() {
        List<NfrTextInfoPanel.Line> lines = new ArrayList<>();
        lines.add(NfrTextInfoPanel.line(tr("neofontrender.gui.licenses.title"), 0xFFFFFF));
        lines.add(NfrTextInfoPanel.spaced(tr("neofontrender.gui.licenses.notice"), 0xBFC7D1));
        try {
            String section = "";
            for (ThirdPartyLicenseCatalog.Entry entry : ThirdPartyLicenseCatalog.load(
                    NfrLicensesSettingsView.class.getClassLoader())) {
                if (!section.equals(entry.section)) {
                    section = entry.section;
                    lines.add(NfrTextInfoPanel.spaced(tr("neofontrender.gui.licenses.section." + section), 0x00DCE8));
                }
                lines.add(NfrTextInfoPanel.line(entry.name + (entry.version.isEmpty() ? "" : " " + entry.version)
                        + " — " + entry.license, 0xD8D8D8));
            }
        } catch (java.io.IOException exception) {
            lines.add(NfrTextInfoPanel.spaced(tr("neofontrender.gui.licenses.load_error"), 0xFF9999));
            neofontrender.NeoFontRender.LOGGER.warn("Could not read third-party license catalogs", exception);
        }
        appendContributions(lines);
        return new NfrTextInfoPanel(lines, true);
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
