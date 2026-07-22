package neofontrender.addons.input;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

final class TextInputSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":text_input"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.input.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1020; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean original = TextInputConfig.iBeamCursor;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid().add(c.toggleText(
                    () -> tr("gui.input.ibeam"), () -> tr("tooltip.input.ibeam"),
                    () -> TextInputConfig.iBeamCursor,
                    value -> { TextInputConfig.iBeamCursor = value; if (!value) TextCursorManager.restoreDefault(); }));
            return new PageView(grid);
        }

        @Override public void apply() { TextInputConfig.save(); }
        @Override public void cancel() { TextInputConfig.iBeamCursor = original; }
    }

    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements." + key); }
    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
