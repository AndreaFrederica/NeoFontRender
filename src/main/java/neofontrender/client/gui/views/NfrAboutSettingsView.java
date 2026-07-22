package neofontrender.client.gui.views;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.Loader;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.component.base.NfrTextInfoPanel;
import neofontrender.client.gui.model.NfrSettingsDraft;
import neofontrender.api.client.settings.NfrInfoLine;
import neofontrender.api.client.settings.NfrInfoPage;
import neofontrender.api.client.settings.NfrInfoPageContribution;
import neofontrender.api.client.settings.NfrInfoPageRegistry;

import java.util.ArrayList;
import java.util.List;

/** About route. Its page-specific copy belongs here, not in the component library. */
public final class NfrAboutSettingsView extends NfrContentView<NfrAboutSettingsView> {
    public NfrAboutSettingsView(NfrSettingsDraft draft, boolean skiaAvailable) {
        this(content(draft, skiaAvailable));
    }

    private NfrAboutSettingsView(NfrTextInfoPanel content) {
        super(section(content, width -> content.preferredHeight()));
    }

    private static NfrTextInfoPanel content(NfrSettingsDraft draft, boolean skiaAvailable) {
        List<NfrTextInfoPanel.Line> lines = new ArrayList<>();
        lines.add(NfrTextInfoPanel.line(tr("neofontrender.gui.about.name"), 0xFFFFFF));
        lines.add(NfrTextInfoPanel.line(() -> tr("neofontrender.gui.about.version") + ": " + version(), 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line(() -> tr("neofontrender.gui.option.engine") + ": "
                + NfrSettingsControls.engineName(draft.engine, skiaAvailable), 0xD8D8D8));
        lines.add(NfrTextInfoPanel.spaced(tr("neofontrender.gui.about.description"), 0xBFC7D1));
        lines.add(NfrTextInfoPanel.line(tr("neofontrender.gui.about.license") + ": MIT License", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("Copyright (c) 2026 AndreaFrederica", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line(tr("neofontrender.gui.about.contributors") + ":", 0xBFC7D1));
        lines.add(NfrTextInfoPanel.line("AndreaFrederica", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("baka-gourd", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("DHJComical", 0xD8D8D8));
        lines.add(NfrTextInfoPanel.line("github.com/AndreaFrederica/NeoFontRender", 0x00DCE8));
        appendContributions(lines, NfrInfoPage.ABOUT);
        return new NfrTextInfoPanel(lines);
    }

    private static void appendContributions(List<NfrTextInfoPanel.Line> target, NfrInfoPage page) {
        for (NfrInfoPageContribution contribution : NfrInfoPageRegistry.snapshot(page)) {
            List<NfrInfoLine> contributed = contribution.lines();
            if (contributed == null) continue;
            for (NfrInfoLine line : contributed) {
                if (line != null) target.add(NfrTextInfoPanel.line(line.text(), line.color(), line.gapBefore()));
            }
        }
    }

    private static String version() {
        return Loader.instance().getIndexedModList().containsKey("neofontrender")
                ? Loader.instance().getIndexedModList().get("neofontrender").getVersion()
                : "unknown";
    }

    private static String tr(String key) {
        return I18n.format(key);
    }
}
