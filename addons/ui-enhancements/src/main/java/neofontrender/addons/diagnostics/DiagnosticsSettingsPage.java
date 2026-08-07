package neofontrender.addons.diagnostics;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.compat.CompatImpact;
import neofontrender.addons.compat.ModCompat;
import neofontrender.addons.compat.ModCompatRegistry;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrTextInfoPanel;
import neofontrender.client.gui.views.NfrContentView;

import java.util.ArrayList;
import java.util.List;

final class DiagnosticsSettingsPage implements NfrSettingsPage {
    @Override
    public String id() {
        return NfrUiEnhancements.MOD_ID + ":diagnostics";
    }

    @Override
    public String titleKey() {
        return "neofontrender_ui_enhancements.gui.diagnostics.category";
    }

    @Override
    public String title() {
        return AddonI18n.tr(titleKey());
    }

    @Override
    public int order() {
        return 1999;
    }

    @Override
    public NfrSettingsPageSession createSession() {
        return new Session();
    }

    private static final class Session implements NfrSettingsPageSession {
        @Override
        public IWidget createView(NfrSettingsPageContext context) {
            return new PageView(buildLines());
        }

        @Override
        public void apply() {}

        @Override
        public void cancel() {}
    }

    private static List<NfrTextInfoPanel.Line> buildLines() {
        List<NfrTextInfoPanel.Line> lines = new ArrayList<>();
        lines.add(NfrTextInfoPanel.spaced(tr("gui.diagnostics.description"), 0xBFC7D1));

        List<ModCompat> active = ModCompatRegistry.active();
        if (active.isEmpty()) {
            lines.add(NfrTextInfoPanel.line(tr("gui.diagnostics.no_compat"), 0x55FF55));
        } else {
            lines.add(NfrTextInfoPanel.spaced(tr("gui.diagnostics.active_compat"), 0xFFFFFF));
            for (ModCompat compat : active) {
                lines.add(NfrTextInfoPanel.line(
                        "  \u2022 " + compat.displayName() + " (" + compat.id() + ")", 0xFFAA55));
                for (CompatImpact impact : compat.impacts()) {
                    String text = "      - " + impactText(impact);
                    lines.add(NfrTextInfoPanel.line(text, 0xD8D8D8));
                }
            }
        }

        lines.add(NfrTextInfoPanel.spaced(tr("gui.diagnostics.all_compat"), 0xFFFFFF));
        for (ModCompat compat : ModCompatRegistry.all()) {
            boolean isActive = compat.isActive();
            String marker = isActive ? "\u00a7a[\u2713]" : "\u00a77[-]";
            lines.add(NfrTextInfoPanel.line(
                    "  " + marker + " \u00a7r" + compat.displayName(),
                    isActive ? 0x55FF55 : 0x888888));
        }

        return lines;
    }

    private static String impactText(CompatImpact impact) {
        if (CompatImpact.KIND_DISABLED_MIXIN.equals(impact.kind)) {
            String simpleName = impact.target.substring(impact.target.lastIndexOf('.') + 1);
            return tr("gui.diagnostics.impact.disabled_mixin")
                    .replace("{mixin}", simpleName)
                    .replace("{reason}", AddonI18n.tr(impact.reasonKey));
        }
        return impact.target;
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(List<NfrTextInfoPanel.Line> lines) {
            NfrTextInfoPanel panel = new NfrTextInfoPanel(lines);
            super(section(panel, width -> panel.preferredHeight()));
        }
    }
}
