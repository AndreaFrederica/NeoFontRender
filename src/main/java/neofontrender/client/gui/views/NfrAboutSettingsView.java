package neofontrender.client.gui.views;

import net.minecraft.client.resources.I18n;
import cpw.mods.fml.common.Loader;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.component.base.NfrTextInfoPanel;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** About route. Its page-specific copy belongs here, not in the component library. */
public final class NfrAboutSettingsView extends NfrContentView<NfrAboutSettingsView> {
    public NfrAboutSettingsView(NfrSettingsDraft draft, boolean skiaAvailable) {
        this(content(draft, skiaAvailable));
    }

    private NfrAboutSettingsView(NfrTextInfoPanel content) {
        super(section(content, width -> content.preferredHeight()));
    }

    private static NfrTextInfoPanel content(NfrSettingsDraft draft, boolean skiaAvailable) {
        return new NfrTextInfoPanel(
                NfrTextInfoPanel.line(tr("neofontrender.gui.about.name"), 0xFFFFFF),
                NfrTextInfoPanel.line(() -> tr("neofontrender.gui.about.version") + ": " + version(), 0xD8D8D8),
                NfrTextInfoPanel.line(() -> tr("neofontrender.gui.option.engine") + ": "
                        + NfrSettingsControls.engineName(draft.engine, skiaAvailable), 0xD8D8D8),
                NfrTextInfoPanel.spaced(tr("neofontrender.gui.about.description"), 0xBFC7D1),
                NfrTextInfoPanel.line(tr("neofontrender.gui.about.license") + ": MIT License", 0xD8D8D8),
                NfrTextInfoPanel.line(tr("neofontrender.gui.about.contributors") + ":", 0xBFC7D1),
                NfrTextInfoPanel.line("AndreaFrederica", 0xD8D8D8),
                NfrTextInfoPanel.line("baka-gourd", 0xD8D8D8),
                NfrTextInfoPanel.line("DHJComical", 0xD8D8D8),
                NfrTextInfoPanel.line("github.com/AndreaFrederica/NeoFontRender", 0x00DCE8));
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
