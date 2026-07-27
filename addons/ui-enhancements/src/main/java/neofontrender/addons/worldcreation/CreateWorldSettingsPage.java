package neofontrender.addons.worldcreation;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.Arrays;

final class CreateWorldSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":create_world"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.create_world.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1042; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final String theme = CreateWorldConfig.theme;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid grid = controls.grid().add(controls.dropdownText(
                    "create_world_theme",
                    () -> tr("gui.create_world.theme"),
                    () -> CreateWorldConfig.theme,
                    value -> CreateWorldConfig.theme = CreateWorldTheme.parse(value).id(),
                    Arrays.asList("vanilla", "tabbed", "modernui"),
                    value -> tr("gui.create_world.theme." + value)).size(260, 24));
            return new PageView(grid);
        }

        @Override public void apply() { CreateWorldConfig.save(); }
        @Override public void cancel() { CreateWorldConfig.theme = theme; }
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
