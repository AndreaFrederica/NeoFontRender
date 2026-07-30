package neofontrender.addons.mainmenu;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

final class MainMenuSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":main_menu"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.main_menu.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1041; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean continueGame = MainMenuConfig.continueGame;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid grid = controls.grid().add(controls.toggleText(
                    () -> tr("gui.main_menu.continue_game"),
                    () -> tr("tooltip.main_menu.continue_game"),
                    () -> MainMenuConfig.continueGame,
                    value -> MainMenuConfig.continueGame = value));
            return new PageView(grid);
        }

        @Override public void apply() { MainMenuConfig.saveSetting(); }
        @Override public void cancel() { MainMenuConfig.continueGame = continueGame; }
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
