package neofontrender.addons.loading;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

final class ResourceReloadSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":resource_reload"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.resource_reload.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1041; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean enabled = ResourceReloadConfig.enabled;
        private final boolean languageSwitch = ResourceReloadConfig.languageSwitch;
        private final boolean resourcePackSwitch = ResourceReloadConfig.resourcePackSwitch;
        private final boolean progressBar = ResourceReloadConfig.progressBar;
        private final boolean percentage = ResourceReloadConfig.percentage;
        private final boolean spinner = ResourceReloadConfig.spinner;
        private final int accentColor = ResourceReloadConfig.accentColor;
        private final int textColor = ResourceReloadConfig.textColor;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.resource_reload.enabled"),
                            () -> tr("tooltip.resource_reload.enabled"),
                            () -> ResourceReloadConfig.enabled, value -> ResourceReloadConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.resource_reload.language"), () -> "",
                            () -> ResourceReloadConfig.languageSwitch,
                            value -> ResourceReloadConfig.languageSwitch = value))
                    .add(c.toggleText(() -> tr("gui.resource_reload.resource_packs"), () -> "",
                            () -> ResourceReloadConfig.resourcePackSwitch,
                            value -> ResourceReloadConfig.resourcePackSwitch = value))
                    .add(c.toggleText(() -> tr("gui.resource_reload.progress_bar"), () -> "",
                            () -> ResourceReloadConfig.progressBar,
                            value -> ResourceReloadConfig.progressBar = value))
                    .add(c.toggleText(() -> tr("gui.resource_reload.percentage"),
                            () -> tr("tooltip.resource_reload.percentage"),
                            () -> ResourceReloadConfig.percentage,
                            value -> ResourceReloadConfig.percentage = value))
                    .add(c.toggleText(() -> tr("gui.resource_reload.spinner"), () -> "",
                            () -> ResourceReloadConfig.spinner,
                            value -> ResourceReloadConfig.spinner = value))
                    .add(c.colorText("resource_reload_accent", () -> tr("gui.resource_reload.accent_color"),
                            () -> ResourceReloadConfig.accentColor,
                            value -> ResourceReloadConfig.accentColor = value, true).size(260, 24))
                    .add(c.colorText("resource_reload_text", () -> tr("gui.resource_reload.text_color"),
                            () -> ResourceReloadConfig.textColor,
                            value -> ResourceReloadConfig.textColor = value, true).size(260, 24));
            return new PageView(grid);
        }

        @Override public void apply() { ResourceReloadConfig.save(); }

        @Override public void cancel() {
            ResourceReloadConfig.enabled = enabled;
            ResourceReloadConfig.languageSwitch = languageSwitch;
            ResourceReloadConfig.resourcePackSwitch = resourcePackSwitch;
            ResourceReloadConfig.progressBar = progressBar;
            ResourceReloadConfig.percentage = percentage;
            ResourceReloadConfig.spinner = spinner;
            ResourceReloadConfig.accentColor = accentColor;
            ResourceReloadConfig.textColor = textColor;
        }
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
