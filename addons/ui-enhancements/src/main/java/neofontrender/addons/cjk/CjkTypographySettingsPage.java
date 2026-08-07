package neofontrender.addons.cjk;

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

final class CjkTypographySettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":cjk_typography"; }
    @Override public String titleKey() {
        return "neofontrender_ui_enhancements.gui.cjk_typography.category";
    }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1004; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final String originalEngine = CjkTypographyConfig.engine;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid grid = controls.grid().add(controls.dropdownText(
                    "cjk_typography_engine",
                    () -> tr("gui.cjk_typography.engine"),
                    () -> CjkTypographyConfig.engine,
                    value -> CjkTypographyConfig.engine = CjkTypographyConfig.normalize(value),
                    Arrays.asList(CjkTypographyConfig.ENGINE_TIQIAN,
                            CjkTypographyConfig.ENGINE_LEGACY),
                    value -> tr("gui.cjk_typography.engine." + value)).size(260, 24));
            return new PageView(grid);
        }

        @Override public void apply() { CjkTypographyConfig.save(); }

        @Override public void cancel() {
            CjkTypographyConfig.engine = originalEngine;
            TiqianParagraphProvider.INSTANCE.clearCache();
        }
    }

    private static String tr(String suffix) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + suffix);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
